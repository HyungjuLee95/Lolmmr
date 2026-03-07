package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;
import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.entity.SummonerHistory;
import com.example.mmrtest.repository.SummonerHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SummonerService {

    @Autowired
    private SummonerHistoryRepository historyRepository;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private RiotMatchService riotMatchService;

    @Value("${riot.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public int convertTierToMmr(String tier, String rank) {
        if (tier == null || tier.equals("UNRANKED") || tier.isEmpty()) {
            return 1000;
        }

        Map<String, Integer> base = Map.of(
                "IRON", 500,
                "BRONZE", 700,
                "SILVER", 900,
                "GOLD", 1100,
                "PLATINUM", 1300,
                "EMERALD", 1500,
                "DIAMOND", 1700,
                "MASTER", 1900,
                "GRANDMASTER", 2100,
                "CHALLENGER", 2300
        );
        Map<String, Integer> offset = Map.of(
                "IV", 0,
                "III", 50,
                "II", 100,
                "I", 150
        );

        return base.getOrDefault(tier.toUpperCase(Locale.ROOT), 1000)
                + offset.getOrDefault(rank.toUpperCase(Locale.ROOT), 0);
    }

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
        ResponseEntity<T> resp = restTemplate.exchange(URI.create(url), HttpMethod.GET, entity, responseType);
        return resp.getBody();
    }

    private Map<String, Object> getAccountByRiotId(String gameName, String tagLine) {
        String encodedGameName = UriUtils.encodePathSegment(gameName, StandardCharsets.UTF_8);
        String[] tagCandidates = new String[]{
                tagLine,
                tagLine.toLowerCase(Locale.ROOT),
                tagLine.toUpperCase(Locale.ROOT)
        };

        for (String candidate : tagCandidates) {
            try {
                String encodedTagLine = UriUtils.encodePathSegment(candidate, StandardCharsets.UTF_8);
                String accountUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                        + encodedGameName + "/" + encodedTagLine;
                Map<String, Object> accountResponse = riotGet(accountUrl, Map.class);
                if (accountResponse != null && accountResponse.get("puuid") != null) {
                    return accountResponse;
                }
            } catch (HttpClientErrorException.NotFound ignored) {
                // 태그라인 케이스 불일치 fallback
            }
        }

        throw new RuntimeException("해당 닉네임의 계정을 찾을 수 없습니다.");
    }

    @Cacheable(value = "summonerInfo", key = "#gameName + '-' + #tagLine", cacheManager = "cacheManager")
    public SummonerDTO getSummonerInfo(String gameName, String tagLine) {
        Map<String, Object> accountResponse = getAccountByRiotId(gameName, tagLine);

        String puuid = (String) accountResponse.get("puuid");
        String accountByPuuidUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-puuid/" + puuid;
        Map<String, Object> accountByPuuidResponse = riotGet(accountByPuuidUrl, Map.class);

        String realName = accountByPuuidResponse != null && accountByPuuidResponse.get("gameName") != null
                ? (String) accountByPuuidResponse.get("gameName")
                : (String) accountResponse.get("gameName");

        String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid;
        Map<String, Object> summonerMap = riotGet(summonerUrl, Map.class);

        SummonerDTO summoner = new SummonerDTO();
        summoner.setPuuid(puuid);
        summoner.setName(StringUtils.hasText(realName) ? realName : gameName);

        if (summonerMap != null) {
            summoner.setId((String) summonerMap.get("id"));

            Number summonerLevel = (Number) summonerMap.get("summonerLevel");
            if (summonerLevel != null) {
                summoner.setSummonerLevel(summonerLevel.intValue());
            }

            Number profileIconId = (Number) summonerMap.get("profileIconId");
            if (profileIconId != null) {
                summoner.setProfileIconId(profileIconId.intValue());
            }
        }

        String leagueUrl = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid;
        Object[] leagueResponse = riotGet(leagueUrl, Object[].class);

        if (leagueResponse != null && leagueResponse.length > 0) {
            for (Object obj : leagueResponse) {
                Map<String, Object> data = (Map<String, Object>) obj;
                String queueType = (String) data.get("queueType");
                String tier = (String) data.get("tier");
                String rankStr = (String) data.get("rank");
                Number lpNumber = (Number) data.get("leaguePoints");
                int lp = lpNumber != null ? lpNumber.intValue() : 0;

                if ("RANKED_SOLO_5x5".equals(queueType)) {
                    summoner.setSoloRank(new SummonerDTO.RankInfo(tier, rankStr, lp));
                    summoner.setTier(tier);
                    summoner.setRank(rankStr);
                    summoner.setLeaguePoints(lp);
                } else if ("RANKED_FLEX_SR".equals(queueType)) {
                    summoner.setFlexRank(new SummonerDTO.RankInfo(tier, rankStr, lp));
                }
            }
        }

        return summoner;
    }

    public Map<String, Object> getMmrAnalysis(String gameName, String tagLine) {
        return getMmrAnalysis(gameName, tagLine, "both");
    }

    public Map<String, Object> getMmrAnalysis(String gameName, String tagLine, String queue) {
        SummonerDTO currentSummoner = getSummonerInfo(gameName, tagLine);
        String puuid = currentSummoner.getPuuid();

        String normalizedQueue = normalizeQueue(queue);
        boolean needSolo = "solo".equals(normalizedQueue) || "both".equals(normalizedQueue);
        boolean needFlex = "flex".equals(normalizedQueue) || "both".equals(normalizedQueue);

        List<String> soloMatchIds = Collections.emptyList();
        List<String> flexMatchIds = Collections.emptyList();

        if (needSolo && needFlex) {
            CompletableFuture<List<String>> soloIdsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchIds(puuid, 420), executorService);
            CompletableFuture<List<String>> flexIdsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchIds(puuid, 440), executorService);

            soloMatchIds = soloIdsFuture.join();
            flexMatchIds = flexIdsFuture.join();
        } else if (needSolo) {
            soloMatchIds = riotMatchService.getMatchIds(puuid, 420);
        } else if (needFlex) {
            flexMatchIds = riotMatchService.getMatchIds(puuid, 440);
        }

        final List<String> resolvedSoloMatchIds = soloMatchIds;
        final List<String> resolvedFlexMatchIds = flexMatchIds;

        List<MatchSummary> soloMatchDetails = Collections.emptyList();
        List<MatchSummary> flexMatchDetails = Collections.emptyList();

        if (needSolo && needFlex) {
            CompletableFuture<List<MatchSummary>> soloDetailsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchSummaries(puuid, resolvedSoloMatchIds, 420), executorService);
            CompletableFuture<List<MatchSummary>> flexDetailsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchSummaries(puuid, resolvedFlexMatchIds, 440), executorService);

            soloMatchDetails = soloDetailsFuture.join();
            flexMatchDetails = flexDetailsFuture.join();
        } else if (needSolo) {
            soloMatchDetails = riotMatchService.getMatchSummaries(puuid, resolvedSoloMatchIds, 420);
        } else if (needFlex) {
            flexMatchDetails = riotMatchService.getMatchSummaries(puuid, resolvedFlexMatchIds, 440);
        }

        ScoreResult soloScoreResult = scoreEngine.calculateScore(soloMatchDetails, 1000);
        ScoreResult flexScoreResult = scoreEngine.calculateScore(flexMatchDetails, 1000);

        int soloLpChange = calculateSoloLpChange(currentSummoner, puuid);
        int flexLpChange = 0;

        saveSoloHistory(currentSummoner, puuid);

        Map<String, Object> result = new HashMap<>();
        result.put("summoner", currentSummoner);
        result.put("soloMatchDetails", soloMatchDetails);
        result.put("flexMatchDetails", flexMatchDetails);
        result.put("soloLpChange", soloLpChange);
        result.put("flexLpChange", flexLpChange);
        result.put("soloScoreResult", soloScoreResult);
        result.put("flexScoreResult", flexScoreResult);
        result.put("requestedQueue", queue);
        result.put("resolvedQueue", normalizedQueue);

        Map<String, Object> counts = new HashMap<>();
        counts.put("solo", buildQueueCounts(soloMatchDetails));
        counts.put("flex", buildQueueCounts(flexMatchDetails));
        result.put("counts", counts);

        result.put("soloSummary", buildQueueSummary(soloMatchDetails));
        result.put("flexSummary", buildQueueSummary(flexMatchDetails));

        return result;
    }

    private Map<String, Object> buildQueueSummary(List<MatchSummary> matchDetails) {
        Map<String, Object> summary = new HashMap<>();

        int wins = 0;
        int losses = 0;
        int remakes = 0;
        int invalid = 0;

        int totalKills = 0;
        int totalDeaths = 0;
        int totalAssists = 0;
        int countedGames = 0;

        if (matchDetails != null) {
            for (MatchSummary match : matchDetails) {
                if (match == null) {
                    continue;
                }

                if (match.isRemake()) {
                    remakes++;
                    continue;
                }

                if (match.isInvalid()) {
                    invalid++;
                    continue;
                }

                if (!match.isCountedGame()) {
                    continue;
                }

                countedGames++;

                if (match.isWin()) {
                    wins++;
                } else {
                    losses++;
                }

                totalKills += match.getKills();
                totalDeaths += match.getDeaths();
                totalAssists += match.getAssists();
            }
        }

        int winRate = countedGames == 0
                ? 0
                : (int) Math.round((wins * 100.0) / countedGames);

        double kdaValue = countedGames == 0
                ? 0.0
                : (totalDeaths == 0
                    ? (totalKills + totalAssists)
                    : ((double) (totalKills + totalAssists) / totalDeaths));

        summary.put("wins", wins);
        summary.put("losses", losses);
        summary.put("remakes", remakes);
        summary.put("invalid", invalid);
        summary.put("countedGames", countedGames);
        summary.put("totalGames", matchDetails == null ? 0 : matchDetails.size());
        summary.put("winRate", winRate);
        summary.put("kda", String.format(Locale.US, "%.2f", kdaValue));

        return summary;
    }

    private Map<String, Object> buildQueueCounts(List<MatchSummary> matchDetails) {
        int total = 0;
        int counted = 0;
        int remakes = 0;
        int invalid = 0;

        if (matchDetails != null) {
            for (MatchSummary match : matchDetails) {
                if (match == null) {
                    continue;
                }

                total++;

                if (match.isRemake()) {
                    remakes++;
                } else if (match.isInvalid()) {
                    invalid++;
                } else if (match.isCountedGame()) {
                    counted++;
                }
            }
        }

        Map<String, Object> counts = new HashMap<>();
        counts.put("total", total);
        counts.put("counted", counted);
        counts.put("remakes", remakes);
        counts.put("invalid", invalid);
        return counts;
    }

    private String normalizeQueue(String queue) {
        String normalizedQueue = queue == null ? "solo" : queue.toLowerCase(Locale.ROOT);
        if (!Set.of("solo", "flex", "both").contains(normalizedQueue)) {
            return "solo";
        }
        return normalizedQueue;
    }

    private int calculateSoloLpChange(SummonerDTO currentSummoner, String puuid) {
        Optional<SummonerHistory> lastRecord = historyRepository.findFirstByPuuidOrderBySearchTimeDesc(puuid);
        if (currentSummoner.getSoloRank() != null
                && lastRecord.isPresent()
                && lastRecord.get().getTier().equals(currentSummoner.getSoloRank().getTier())) {
            return currentSummoner.getSoloRank().getLeaguePoints() - lastRecord.get().getLeaguePoints();
        }
        return 0;
    }

    private void saveSoloHistory(SummonerDTO currentSummoner, String puuid) {
        if (currentSummoner.getSoloRank() == null) {
            return;
        }

        SummonerHistory history = new SummonerHistory();
        history.setPuuid(puuid);
        history.setTier(currentSummoner.getSoloRank().getTier());
        history.setRank(currentSummoner.getSoloRank().getRank());
        history.setLeaguePoints(currentSummoner.getSoloRank().getLeaguePoints());
        history.setSearchTime(LocalDateTime.now());
        historyRepository.save(history);
    }
}