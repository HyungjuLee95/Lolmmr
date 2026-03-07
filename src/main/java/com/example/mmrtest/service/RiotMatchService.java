package com.example.mmrtest.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.mmrtest.dto.MatchResultType;
import com.example.mmrtest.dto.MatchSummary;

@Service
public class RiotMatchService {

    @Value("${riot.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private <T> T riotGet(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);
        headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36");
        headers.set("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7");
        headers.set("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
        headers.set("Origin", "https://developer.riotgames.com");
        headers.set("Accept", "application/json");

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
                .toList();

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
            if (response == null) {
                return null;
            }

            Map<String, Object> info = asMap(response.get("info"));
            if (info == null) {
                return null;
            }

            int queueId = getInt(info, "queueId", 0);
            if (expectedQueueId != null && !expectedQueueId.equals(queueId)) {
                return null;
            }

            List<Map<String, Object>> participants = asListOfMaps(info.get("participants"));
            if (participants.isEmpty()) {
                return null;
            }

            for (Map<String, Object> p : participants) {
                if (!puuid.equals(getString(p, "puuid", ""))) {
                    continue;
                }

                int myTeamId = getInt(p, "teamId", 0);

                List<Map<String, Object>> sortedParts = participants.stream()
                        .sorted((p1, p2) -> {
                            int t1 = getInt(p1, "teamId", 0);
                            int t2 = getInt(p2, "teamId", 0);
                            if (t1 == myTeamId && t2 != myTeamId) return -1;
                            if (t1 != myTeamId && t2 == myTeamId) return 1;
                            return 0;
                        })
                        .toList();

                List<String> teamMembers = sortedParts.stream()
                        .map(this::buildPlayerDisplayName)
                        .toList();

                List<String> teamChamps = sortedParts.stream()
                        .map(part -> getString(part, "championName", "Unknown"))
                        .toList();

                int totalCs = getInt(p, "totalMinionsKilled", 0)
                        + getInt(p, "neutralMinionsKilled", 0);
                int goldEarned = getInt(p, "goldEarned", 0);
                long gameEndTimeStamp = getLong(info, "gameEndTimestamp", 0L);

                int rawDuration = getInt(info, "gameDuration", 0);
                if (rawDuration > 10000) {
                    rawDuration = rawDuration / 1000;
                }
                int duration = rawDuration / 60;

                List<Integer> items = Arrays.asList(
                        getInt(p, "item0", 0),
                        getInt(p, "item1", 0),
                        getInt(p, "item2", 0),
                        getInt(p, "item3", 0),
                        getInt(p, "item4", 0),
                        getInt(p, "item5", 0),
                        getInt(p, "item6", 0)
                );

                int kills = getInt(p, "kills", 0);
                int deaths = getInt(p, "deaths", 0);
                int assists = getInt(p, "assists", 0);

                MatchResultType resultType = resolveResultType(info, p);

                int performanceScore = (resultType == MatchResultType.WIN || resultType == MatchResultType.LOSS)
                        ? (kills + assists) - deaths
                        : 0;

                return new MatchSummary(
                        matchId,
                        resultType,
                        kills,
                        deaths,
                        assists,
                        getString(p, "championName", "Unknown"),
                        items,
                        teamMembers,
                        teamChamps,
                        duration,
                        getInt(p, "summoner1Id", 0),
                        getInt(p, "summoner2Id", 0),
                        extractMainRuneId(p),
                        extractSubRuneId(p),
                        totalCs,
                        goldEarned,
                        queueId,
                        gameEndTimeStamp,
                        performanceScore
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public MatchSummary fetchMatchDetail(String puuid, String matchId) {
        return fetchMatchDetail(puuid, matchId, null);
    }

    private MatchResultType resolveResultType(Map<String, Object> info, Map<String, Object> participant) {
        if (getBoolean(participant, "gameEndedInEarlySurrender", false)) {
            return MatchResultType.REMAKE;
        }

        Object winValue = participant.get("win");
        if (winValue instanceof Boolean win) {
            return win ? MatchResultType.WIN : MatchResultType.LOSS;
        }

        String endOfGameResult = getString(info, "endOfGameResult", "");
        if (!endOfGameResult.isBlank() && endOfGameResult.toLowerCase().contains("abort")) {
            return MatchResultType.INVALID;
        }

        return MatchResultType.INVALID;
    }

    private String buildPlayerDisplayName(Map<String, Object> participant) {
        String riotIdGameName = getString(participant, "riotIdGameName", "");
        String riotIdTagline = getString(participant, "riotIdTagline", "");

        if (!riotIdGameName.isBlank() && !riotIdTagline.isBlank()) {
            return riotIdGameName + "#" + riotIdTagline;
        }
        if (!riotIdGameName.isBlank()) {
            return riotIdGameName;
        }

        String summonerName = getString(participant, "summonerName", "");
        return summonerName.isBlank() ? "Unknown Player" : summonerName;
    }

    private int extractMainRuneId(Map<String, Object> participant) {
        Map<String, Object> perks = asMap(participant.get("perks"));
        if (perks == null) {
            return 0;
        }

        List<Map<String, Object>> styles = asListOfMaps(perks.get("styles"));
        if (styles.isEmpty()) {
            return 0;
        }

        Map<String, Object> primaryStyle = styles.get(0);
        List<Map<String, Object>> selections = asListOfMaps(primaryStyle.get("selections"));
        if (selections.isEmpty()) {
            return 0;
        }

        return getInt(selections.get(0), "perk", 0);
    }

    private int extractSubRuneId(Map<String, Object> participant) {
        Map<String, Object> perks = asMap(participant.get("perks"));
        if (perks == null) {
            return 0;
        }

        List<Map<String, Object>> styles = asListOfMaps(perks.get("styles"));
        if (styles.size() < 2) {
            return 0;
        }

        return getInt(styles.get(1), "style", 0);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return defaultValue;
    }
}