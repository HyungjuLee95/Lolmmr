package com.example.mmrtest.service;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
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

    private static final int SCORE_SAMPLE_MATCH_LIMIT = 20;
    private static final int DISPLAY_MATCH_LIMIT = 5;
    private static final int SOLO_QUEUE_ID = 420;
    private static final int FLEX_QUEUE_ID = 440;

    @Autowired
    private SummonerHistoryRepository historyRepository;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private RiotMatchService riotMatchService;

    @Autowired
    private RiotApiClient riotApiClient;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 컨트롤러에서 standardMmr 용도로도 사용 중이므로 유지.
     * 기존 500~2300 기반이 아니라, 현재 프로젝트의 자체 점수 구간 기준으로 맞춘다.
     */
    public int convertTierToMmr(String tier, String rank) {
        if (tier == null || tier.isBlank() || "UNRANKED".equalsIgnoreCase(tier)) {
            return 1000;
        }

        Map<String, Integer> base = Map.of(
                "IRON", 700,
                "BRONZE", 900,
                "SILVER", 1100,
                "GOLD", 1450,
                "PLATINUM", 1750,
                "EMERALD", 2050,
                "DIAMOND", 2300,
                "MASTER", 2450,
                "GRANDMASTER", 2550,
                "CHALLENGER", 2650
        );

        Map<String, Integer> offset = Map.of(
                "IV", 0,
                "III", 40,
                "II", 80,
                "I", 120
        );

        String normalizedTier = tier.toUpperCase(Locale.ROOT);
        String normalizedRank = rank == null ? "" : rank.toUpperCase(Locale.ROOT);

        return base.getOrDefault(normalizedTier, 1000)
                + offset.getOrDefault(normalizedRank, 0);
    }

    private int resolveSeedScore(SummonerDTO.RankInfo rankInfo) {
        if (rankInfo == null || rankInfo.getTier() == null || rankInfo.getTier().isBlank()) {
            return 1000;
        }
        return convertTierToMmr(rankInfo.getTier(), rankInfo.getRank());
    }

    private <T> T riotGet(String url, Class<T> responseType) {
        return riotApiClient.get(url, responseType);
    }

    private <T> T riotGet(String url, ParameterizedTypeReference<T> responseType) {
        return riotApiClient.get(url, responseType);
    }

    private Map<String, Object> riotGetMap(String url) {
        return riotGet(url, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private List<Map<String, Object>> riotGetListOfMaps(String url) {
        List<Map<String, Object>> response = riotGet(url, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
        return response == null ? Collections.emptyList() : response;
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
                Map<String, Object> accountResponse = riotGetMap(accountUrl);
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
        Map<String, Object> accountByPuuidResponse = riotGetMap(accountByPuuidUrl);

        String realName = accountByPuuidResponse != null && accountByPuuidResponse.get("gameName") != null
                ? (String) accountByPuuidResponse.get("gameName")
                : (String) accountResponse.get("gameName");

        String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid;
        Map<String, Object> summonerMap = riotGetMap(summonerUrl);

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
        List<Map<String, Object>> leagueResponse = riotGetListOfMaps(leagueUrl);

        for (Map<String, Object> data : leagueResponse) {
            String queueType = (String) data.get("queueType");
            String tier = (String) data.get("tier");
            String rankStr = (String) data.get("rank");
            Number lpNumber = (Number) data.get("leaguePoints");
            int lp = lpNumber != null ? lpNumber.intValue() : 0;

            if ("RANKED_SOLO_5x5".equals(queueType)) {
                summoner.setSoloRank(new SummonerDTO.RankInfo(tier, rankStr, lp));
            } else if ("RANKED_FLEX_SR".equals(queueType)) {
                summoner.setFlexRank(new SummonerDTO.RankInfo(tier, rankStr, lp));
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
                    () -> riotMatchService.getMatchIds(puuid, SOLO_QUEUE_ID, SCORE_SAMPLE_MATCH_LIMIT),
                    executorService
            );
            CompletableFuture<List<String>> flexIdsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchIds(puuid, FLEX_QUEUE_ID, SCORE_SAMPLE_MATCH_LIMIT),
                    executorService
            );

            soloMatchIds = soloIdsFuture.join();
            flexMatchIds = flexIdsFuture.join();
        } else if (needSolo) {
            soloMatchIds = riotMatchService.getMatchIds(puuid, SOLO_QUEUE_ID, SCORE_SAMPLE_MATCH_LIMIT);
        } else if (needFlex) {
            flexMatchIds = riotMatchService.getMatchIds(puuid, FLEX_QUEUE_ID, SCORE_SAMPLE_MATCH_LIMIT);
        }

        final List<String> resolvedSoloMatchIds = soloMatchIds;
        final List<String> resolvedFlexMatchIds = flexMatchIds;

        List<MatchSummary> soloMatchDetails = Collections.emptyList();
        List<MatchSummary> flexMatchDetails = Collections.emptyList();

        if (needSolo && needFlex) {
            CompletableFuture<List<MatchSummary>> soloDetailsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchSummaries(puuid, resolvedSoloMatchIds, SOLO_QUEUE_ID),
                    executorService
            );
            CompletableFuture<List<MatchSummary>> flexDetailsFuture = CompletableFuture.supplyAsync(
                    () -> riotMatchService.getMatchSummaries(puuid, resolvedFlexMatchIds, FLEX_QUEUE_ID),
                    executorService
            );

            soloMatchDetails = soloDetailsFuture.join();
            flexMatchDetails = flexDetailsFuture.join();
        } else if (needSolo) {
            soloMatchDetails = riotMatchService.getMatchSummaries(puuid, resolvedSoloMatchIds, SOLO_QUEUE_ID);
        } else if (needFlex) {
            flexMatchDetails = riotMatchService.getMatchSummaries(puuid, resolvedFlexMatchIds, FLEX_QUEUE_ID);
        }

        int soloSeedScore = resolveSeedScore(currentSummoner.getSoloRank());
        int flexSeedScore = resolveSeedScore(currentSummoner.getFlexRank());

        ScoreResult soloScoreResult = scoreEngine.calculateScore(soloMatchDetails, soloSeedScore);
        ScoreResult flexScoreResult = scoreEngine.calculateScore(flexMatchDetails, flexSeedScore);

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
        summary.put("displayMatchCount", DISPLAY_MATCH_LIMIT);
        summary.put("scoreSampleCount", SCORE_SAMPLE_MATCH_LIMIT);
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
        counts.put("displayMatchCount", DISPLAY_MATCH_LIMIT);
        counts.put("scoreSampleCount", SCORE_SAMPLE_MATCH_LIMIT);
        return counts;
    }

    private String normalizeQueue(String queue) {
        if (queue == null || queue.isBlank()) {
            return "both";
        }

        String normalized = queue.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("solo", "flex", "both").contains(normalized)) {
            return "both";
        }
        return normalized;
    }

    private int calculateSoloLpChange(SummonerDTO currentSummoner, String puuid) {
        if (currentSummoner.getSoloRank() == null) {
            return 0;
        }

        Optional<SummonerHistory> lastRecord = historyRepository.findFirstByPuuidOrderBySearchTimeDesc(puuid);
        if (lastRecord.isPresent()
                && lastRecord.get().getTier() != null
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