package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class RiotMatchService {

    @Value("${riot.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private <T> T riotGet(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<T> resp = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        return resp.getBody();
    }

    @Cacheable(
            value = "matchIds",
            key = "#puuid + '-' + #queueId",
            cacheManager = "cacheManager",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<String> getMatchIds(String puuid, Integer queueId) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/" + puuid
                + "/ids?start=0&count=100&type=ranked";
        if (queueId != null) {
            url += "&queue=" + queueId;
        }

        String[] matchIds = riotGet(url, String[].class);
        List<String> ids = matchIds != null ? Arrays.asList(matchIds) : new ArrayList<>();

        if (queueId != null && ids.isEmpty()) {
            String fallbackUrl = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/" + puuid
                    + "/ids?start=0&count=100&type=ranked";
            String[] fallbackIds = riotGet(fallbackUrl, String[].class);
            ids = fallbackIds != null ? Arrays.asList(fallbackIds) : new ArrayList<>();
        }

        return ids;
    }

    public List<MatchSummary> getMatchSummaries(String puuid, List<String> matchIds, Integer expectedQueueId) {
        List<CompletableFuture<MatchSummary>> futures = matchIds.stream()
                .map(matchId -> CompletableFuture.supplyAsync(
                        () -> fetchMatchDetail(puuid, matchId, expectedQueueId),
                        executorService
                ))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "matchRaw", key = "#matchId", cacheManager = "cacheManager")
    public Map<String, Object> fetchMatchRaw(String matchId) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId;
        return riotGet(url, Map.class);
    }

    public MatchSummary fetchMatchDetail(String puuid, String matchId, Integer expectedQueueId) {
        try {
            Map<String, Object> response = fetchMatchRaw(matchId);
            if (response == null) return null;

            Map<String, Object> info = (Map<String, Object>) response.get("info");
            if (info == null) return null;

            if (expectedQueueId != null) {
                Number queueNumber = (Number) info.get("queueId");
                if (queueNumber == null || !expectedQueueId.equals(queueNumber.intValue())) {
                    return null;
                }
            }

            List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");
            if (participants == null) return null;

            for (Map<String, Object> p : participants) {
                if (puuid.equals(p.get("puuid"))) {
                    int myTeamId = ((Number) p.get("teamId")).intValue();

                    List<Map<String, Object>> sortedParts = participants.stream()
                            .sorted((p1, p2) -> {
                                int t1 = ((Number) p1.get("teamId")).intValue();
                                int t2 = ((Number) p2.get("teamId")).intValue();
                                if (t1 == myTeamId && t2 != myTeamId) return -1;
                                if (t1 != myTeamId && t2 == myTeamId) return 1;
                                return 0;
                            }).collect(Collectors.toList());

                    List<String> teamMembers = sortedParts.stream()
                            .map(part -> part.get("riotIdGameName") + "#" + part.get("riotIdTagline"))
                            .collect(Collectors.toList());

                    List<String> teamChamps = sortedParts.stream()
                            .map(part -> (String) part.get("championName"))
                            .collect(Collectors.toList());

                    int totalCs = ((Number) p.get("totalMinionsKilled")).intValue()
                            + ((Number) p.get("neutralMinionsKilled")).intValue();
                    int goldEarned = ((Number) p.get("goldEarned")).intValue();

                    long gameEndTimeStamp = ((Number) info.get("gameEndTimestamp")).longValue();

                    int rawDuration = ((Number) info.get("gameDuration")).intValue();
                    if (rawDuration > 10000) rawDuration = rawDuration / 1000;
                    int duration = rawDuration / 60;

                    List<Integer> items = Arrays.asList(
                            ((Number) p.get("item0")).intValue(),
                            ((Number) p.get("item1")).intValue(),
                            ((Number) p.get("item2")).intValue(),
                            ((Number) p.get("item3")).intValue(),
                            ((Number) p.get("item4")).intValue(),
                            ((Number) p.get("item5")).intValue(),
                            ((Number) p.get("item6")).intValue()
                    );

                    Map<String, Object> perks = (Map<String, Object>) p.get("perks");
                    List<Map<String, Object>> styles = (List<Map<String, Object>>) perks.get("styles");
                    int mainRuneId = ((Number) ((List<Map<String, Object>>) styles.get(0).get("selections"))
                            .get(0).get("perk")).intValue();
                    int subRuneId = ((Number) styles.get(1).get("style")).intValue();

                    int kills = ((Number) p.get("kills")).intValue();
                    int deaths = ((Number) p.get("deaths")).intValue();
                    int assists = ((Number) p.get("assists")).intValue();

                    return new MatchSummary(
                            (boolean) p.get("win"),
                            kills, deaths, assists,
                            (String) p.get("championName"),
                            items, teamMembers, teamChamps, duration,
                            ((Number) p.get("summoner1Id")).intValue(),
                            ((Number) p.get("summoner2Id")).intValue(),
                            mainRuneId, subRuneId,
                            totalCs, goldEarned, gameEndTimeStamp,
                            (kills + assists) - deaths
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public MatchSummary fetchMatchDetail(String puuid, String matchId) {
        return fetchMatchDetail(puuid, matchId, null);
    }
}
