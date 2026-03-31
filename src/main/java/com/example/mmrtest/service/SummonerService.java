package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;
import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.entity.SummonerHistory;
import com.example.mmrtest.entity.core.SummonerProfile;
import com.example.mmrtest.repository.SummonerHistoryRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final Logger log = LoggerFactory.getLogger(SummonerService.class);

    private static final int SOLO_QUEUE_ID = 420;
    private static final int FLEX_QUEUE_ID = 440;

    private static final int SCORE_SAMPLE_MATCH_LIMIT = 20;
    private static final int DISPLAY_MATCH_LIMIT = 5;

    private static final Duration MMR_CACHE_TTL = Duration.ofMinutes(5);
    private static final String MMR_CACHE_PREFIX = "mmr:v1:";
    private static final Duration PROFILE_DB_TTL = Duration.ofMinutes(10);

    @Autowired
    private SummonerHistoryRepository historyRepository;

    @Autowired
    private ScoreEngine scoreEngine;

    @Autowired
    private RiotMatchService riotMatchService;

    @Autowired
    private RiotApiClient riotApiClient;

    @Autowired
    private CoreSnapshotService coreSnapshotService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private Map<Long, String> championIdToNameMap = null;

    private synchronized String resolveChampionName(long championId) {
        if (championIdToNameMap == null) {
            championIdToNameMap = new HashMap<>();
            try {
                String ddragonUrl = "https://ddragon.leagueoflegends.com/cdn/16.6.1/data/en_US/champion.json";
                Map<String, Object> data = new org.springframework.web.client.RestTemplate().getForObject(ddragonUrl, Map.class);
                if (data != null && data.get("data") != null) {
                    Map<String, Map<String, Object>> champs = (Map<String, Map<String, Object>>) data.get("data");
                    for (Map.Entry<String, Map<String, Object>> entry : champs.entrySet()) {
                        String idStr = (String) entry.getValue().get("key");
                        long cId = Long.parseLong(idStr);
                        String nameId = (String) entry.getValue().get("id");
                        championIdToNameMap.put(cId, nameId);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to load DDragon champion.json", e);
            }
        }
        return championIdToNameMap.getOrDefault(championId, "Unknown");
    }

    public int convertTierToMmr(String tier, String rank) {
        if (tier == null || tier.isBlank() || "UNRANKED".equalsIgnoreCase(tier)) {
            return 1000;
        }

        Map<String, Integer> base = Map.of(
                "IRON", 800,
                "BRONZE", 950,
                "SILVER", 1000,
                "GOLD", 1300,
                "PLATINUM", 1600,
                "EMERALD", 1900,
                "DIAMOND", 2200,
                "MASTER", 2350,
                "GRANDMASTER", 2450,
                "CHALLENGER", 2550
        );

        return base.getOrDefault(tier.toUpperCase(Locale.ROOT), 1000);
    }

    private int resolveSeedScore(SummonerDTO.RankInfo rankInfo) {
        if (rankInfo == null || !StringUtils.hasText(rankInfo.getTier())) {
            return 1000;
        }
        return convertTierToMmr(rankInfo.getTier(), rankInfo.getRank());
    }

    private Map<String, Object> riotGetMap(String url) {
        return riotApiClient.get(url, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private List<Map<String, Object>> riotGetListOfMaps(String url) {
        List<Map<String, Object>> response =
                riotApiClient.get(url, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
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
                String accountUrl =
                        "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/"
                                + encodedGameName + "/" + encodedTagLine;

                Map<String, Object> accountResponse = riotGetMap(accountUrl);
                if (accountResponse != null && accountResponse.get("puuid") != null) {
                    return accountResponse;
                }
            } catch (HttpClientErrorException.NotFound ignored) {
                // 태그라인 대소문자 fallback
            }
        }

        throw new RuntimeException("해당 닉네임의 계정을 찾을 수 없습니다.");
    }

    @Cacheable(value = "summonerInfo", key = "#gameName + '-' + #tagLine", cacheManager = "cacheManager")
    public SummonerDTO getSummonerInfo(String gameName, String tagLine) {
        Optional<SummonerProfile> profileOpt = coreSnapshotService.findSummonerProfile(gameName, tagLine);

        if (profileOpt.isPresent()) {
            SummonerProfile profile = profileOpt.get();
            OffsetDateTime lastSync = profile.getLastProfileSyncAt();

            if (lastSync != null && lastSync.isAfter(OffsetDateTime.now().minus(PROFILE_DB_TTL))) {
                log.info("SummonerProfile DB HIT. gameName={}, tagLine={}", gameName, tagLine);

                SummonerDTO dto = new SummonerDTO();
                dto.setPuuid(profile.getPuuid());
                dto.setName(profile.getGameName());
                dto.setSummonerLevel(profile.getSummonerLevel() == null ? 0 : profile.getSummonerLevel());
                dto.setProfileIconId(profile.getProfileIconId() == null ? 0 : profile.getProfileIconId());
                dto.setId(profile.getSummonerId());
                dto.setSoloRank(coreSnapshotService.findLatestRank(profile.getPuuid(), "RANKED_SOLO_5x5"));
                dto.setFlexRank(coreSnapshotService.findLatestRank(profile.getPuuid(), "RANKED_FLEX_SR"));

                return dto;
            }

            log.info("SummonerProfile DB STALE. gameName={}, tagLine={}, fallback=riot", gameName, tagLine);
        } else {
            log.info("SummonerProfile DB MISS. gameName={}, tagLine={}, fallback=riot", gameName, tagLine);
        }

        Map<String, Object> accountResponse = getAccountByRiotId(gameName, tagLine);

        String puuid = (String) accountResponse.get("puuid");
        String accountByPuuidUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-puuid/" + puuid;
        Map<String, Object> accountByPuuidResponse = riotGetMap(accountByPuuidUrl);

        String realName =
                accountByPuuidResponse != null && accountByPuuidResponse.get("gameName") != null
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

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    public Map<String, Object> getMmrAnalysis(String gameName, String tagLine) {
        return getMmrAnalysis(gameName, tagLine, "both", false);
    }

    public Map<String, Object> getMmrAnalysis(String gameName, String tagLine, String queue, boolean forceRefresh) {
        String normalizedQueue = normalizeQueue(queue);
        String cacheKey = buildMmrCacheKey(gameName, tagLine, normalizedQueue);

        if (forceRefresh) {
            log.info("Force refresh requested. Evicting caches for {}#{}", gameName, tagLine);
            redisTemplate.delete(cacheKey);
            org.springframework.cache.Cache cache = cacheManager.getCache("summonerInfo");
            if (cache != null) {
                cache.evict(gameName + "-" + tagLine);
            }
        }

        Map<String, Object> cachedResult = readMmrAnalysisCache(cacheKey);
        if (cachedResult != null) {
            log.info("Redis MMR cache HIT. key={}", cacheKey);
            return cachedResult;
        }

        log.info("Redis MMR cache MISS. key={}", cacheKey);

        SummonerDTO currentSummoner = getSummonerInfo(gameName, tagLine);
        String puuid = currentSummoner.getPuuid();

        boolean needSolo = "solo".equals(normalizedQueue) || "both".equals(normalizedQueue);
        boolean needFlex = "flex".equals(normalizedQueue) || "both".equals(normalizedQueue);

        List<MatchSummary> soloScoreSampleMatches = Collections.emptyList();
        List<MatchSummary> flexScoreSampleMatches = Collections.emptyList();

        if (needSolo && needFlex) {
            CompletableFuture<List<MatchSummary>> soloFuture = CompletableFuture.supplyAsync(
                    () -> loadScoreSampleMatches(puuid, SOLO_QUEUE_ID),
                    executorService
            );
            CompletableFuture<List<MatchSummary>> flexFuture = CompletableFuture.supplyAsync(
                    () -> loadScoreSampleMatches(puuid, FLEX_QUEUE_ID),
                    executorService
            );

            soloScoreSampleMatches = soloFuture.join();
            flexScoreSampleMatches = flexFuture.join();
        } else if (needSolo) {
            soloScoreSampleMatches = loadScoreSampleMatches(puuid, SOLO_QUEUE_ID);
        } else if (needFlex) {
            flexScoreSampleMatches = loadScoreSampleMatches(puuid, FLEX_QUEUE_ID);
        }

        int soloLpChange = needSolo ? calculateSoloLpChange(currentSummoner, puuid) : 0;
        int flexLpChange = 0;

        if (needSolo) {
            saveSoloHistory(currentSummoner, puuid);
        }

        ScoreResult soloScoreResult = needSolo
                ? scoreEngine.calculateScore(soloScoreSampleMatches, resolveSeedScore(currentSummoner.getSoloRank()))
                : scoreEngine.calculateScore(Collections.emptyList(), 1000);

        ScoreResult flexScoreResult = needFlex
                ? scoreEngine.calculateScore(flexScoreSampleMatches, resolveSeedScore(currentSummoner.getFlexRank()))
                : scoreEngine.calculateScore(Collections.emptyList(), 1000);

        List<MatchSummary> soloDisplayMatches = limitForDisplay(soloScoreSampleMatches);
        List<MatchSummary> flexDisplayMatches = limitForDisplay(flexScoreSampleMatches);

        try {
            coreSnapshotService.saveAllSnapshots(
                    gameName,
                    tagLine,
                    currentSummoner,
                    needSolo ? soloScoreSampleMatches : Collections.emptyList(),
                    needFlex ? flexScoreSampleMatches : Collections.emptyList()
            );
        } catch (Exception e) {
            log.warn(
                    "Core snapshot save failed. puuid={}, queue={}, cause={}",
                    puuid,
                    normalizedQueue,
                    e.getMessage()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("summoner", currentSummoner);
        result.put("soloMatchDetails", soloDisplayMatches);
        result.put("flexMatchDetails", flexDisplayMatches);
        result.put("soloLpChange", soloLpChange);
        result.put("flexLpChange", flexLpChange);
        result.put("soloScoreResult", soloScoreResult);
        result.put("flexScoreResult", flexScoreResult);
        result.put("requestedQueue", queue);
        result.put("resolvedQueue", normalizedQueue);

        try {
            String activeGameUrl = "https://kr.api.riotgames.com/lol/spectator/v5/active-games/by-summoner/" + puuid;
            Map<String, Object> activeGame = riotApiClient.getOptional(activeGameUrl, new ParameterizedTypeReference<Map<String, Object>>() {});
            if (activeGame != null) {
                result.put("activeGame", activeGame);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch active game for puuid={}. cause={}", puuid, e.getMessage());
        }

        try {
            String masteryUrl = "https://kr.api.riotgames.com/lol/champion-mastery/v4/champion-masteries/by-puuid/" + puuid + "/top?count=3";
            List<Map<String, Object>> masteries = riotApiClient.getOptional(masteryUrl, new ParameterizedTypeReference<List<Map<String, Object>>>() {});
            if (masteries != null && !masteries.isEmpty()) {
                List<Map<String, Object>> topMasteries = new ArrayList<>();
                for (Map<String, Object> m : masteries) {
                    Map<String, Object> item = new HashMap<>();
                    long championId = ((Number) m.get("championId")).longValue();
                    item.put("championId", championId);
                    item.put("championName", resolveChampionName(championId));
                    item.put("championLevel", m.get("championLevel"));
                    item.put("championPoints", m.get("championPoints"));
                    topMasteries.add(item);
                }
                result.put("championMasteries", topMasteries);
            }
        } catch (Exception e) {
            log.warn("Failed to fetch champion masteries for puuid={}. cause={}", puuid, e.getMessage());
        }

        Map<String, Object> counts = new HashMap<>();
        counts.put("solo", buildQueueCounts(soloScoreSampleMatches));
        counts.put("flex", buildQueueCounts(flexScoreSampleMatches));
        result.put("counts", counts);

        result.put("soloSummary", buildQueueSummary(soloScoreSampleMatches));
        result.put("flexSummary", buildQueueSummary(flexScoreSampleMatches));

        writeMmrAnalysisCache(cacheKey, result);

        return result;
    }

    private List<MatchSummary> loadScoreSampleMatches(String puuid, int queueId) {
        List<MatchSummary> dbMatches = coreSnapshotService.findRecentMatchSummaries(
                puuid,
                queueId,
                SCORE_SAMPLE_MATCH_LIMIT
        );

        if (dbMatches.size() >= SCORE_SAMPLE_MATCH_LIMIT) {
            log.info("DB match summary HIT. puuid={}, queueId={}, count={}", puuid, queueId, dbMatches.size());
            return dbMatches;
        }

        if (!dbMatches.isEmpty()) {
            log.info(
                    "DB match summary PARTIAL HIT. puuid={}, queueId={}, count={}, fallback=riot",
                    puuid,
                    queueId,
                    dbMatches.size()
            );
        } else {
            log.info("DB match summary MISS. puuid={}, queueId={}, fallback=riot", puuid, queueId);
        }

        List<String> matchIds = riotMatchService.getMatchIds(puuid, queueId, SCORE_SAMPLE_MATCH_LIMIT);
        if (matchIds.isEmpty()) {
            return Collections.emptyList();
        }

        return riotMatchService.getMatchSummaries(puuid, matchIds, queueId);
    }

    private List<MatchSummary> limitForDisplay(List<MatchSummary> matches) {
        if (matches == null || matches.isEmpty()) {
            return Collections.emptyList();
        }

        int end = Math.min(DISPLAY_MATCH_LIMIT, matches.size());
        return new ArrayList<>(matches.subList(0, end));
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
        summary.put("scoreCountedGames", countedGames);
        summary.put("excludedGames", remakes + invalid);
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
            return currentSummoner.getSoloRank().getLeaguePoints()
                    - lastRecord.get().getLeaguePoints();
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

    private String buildMmrCacheKey(String gameName, String tagLine, String normalizedQueue) {
        String safeGameName = StringUtils.hasText(gameName)
                ? gameName.trim().toLowerCase(Locale.ROOT)
                : "unknown";
        String safeTagLine = StringUtils.hasText(tagLine)
                ? tagLine.trim().toLowerCase(Locale.ROOT)
                : "unknown";

        return MMR_CACHE_PREFIX + safeGameName + ":" + safeTagLine + ":" + normalizedQueue;
    }

    private Map<String, Object> readMmrAnalysisCache(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(cachedJson)) {
                return null;
            }

            Map<String, Object> raw = objectMapper.readValue(
                    cachedJson,
                    new TypeReference<Map<String, Object>>() {}
            );

            return restoreCachedAnalysisResult(raw);
        } catch (Exception e) {
            log.warn("Redis MMR cache read failed. key={}, cause={}", cacheKey, e.getMessage());
            return null;
        }
    }

    private Map<String, Object> restoreCachedAnalysisResult(Map<String, Object> raw) {
        if (raw == null) {
            return null;
        }

        Map<String, Object> restored = new HashMap<>(raw);

        Object summonerObj = raw.get("summoner");
        if (summonerObj != null) {
            restored.put("summoner", objectMapper.convertValue(summonerObj, SummonerDTO.class));
        }

        Object soloMatchDetailsObj = raw.get("soloMatchDetails");
        if (soloMatchDetailsObj != null) {
            restored.put(
                    "soloMatchDetails",
                    objectMapper.convertValue(
                            soloMatchDetailsObj,
                            new TypeReference<List<MatchSummary>>() {}
                    )
            );
        }

        Object flexMatchDetailsObj = raw.get("flexMatchDetails");
        if (flexMatchDetailsObj != null) {
            restored.put(
                    "flexMatchDetails",
                    objectMapper.convertValue(
                            flexMatchDetailsObj,
                            new TypeReference<List<MatchSummary>>() {}
                    )
            );
        }

        Object soloScoreResultObj = raw.get("soloScoreResult");
        if (soloScoreResultObj != null) {
            restored.put("soloScoreResult", objectMapper.convertValue(soloScoreResultObj, ScoreResult.class));
        }

        Object flexScoreResultObj = raw.get("flexScoreResult");
        if (flexScoreResultObj != null) {
            restored.put("flexScoreResult", objectMapper.convertValue(flexScoreResultObj, ScoreResult.class));
        }

        Object activeGameObj = raw.get("activeGame");
        if (activeGameObj != null) {
            restored.put(
                    "activeGame",
                    objectMapper.convertValue(
                            activeGameObj,
                            new TypeReference<Map<String, Object>>() {}
                    )
            );
        }

        Object championMasteriesObj = raw.get("championMasteries");
        if (championMasteriesObj != null) {
            restored.put(
                    "championMasteries",
                    objectMapper.convertValue(
                            championMasteriesObj,
                            new TypeReference<List<Map<String, Object>>>() {}
                    )
            );
        }

        return restored;
    }

    private void writeMmrAnalysisCache(String cacheKey, Map<String, Object> result) {
        try {
            String payload = objectMapper.writeValueAsString(result);
            redisTemplate.opsForValue().set(cacheKey, payload, MMR_CACHE_TTL);
            log.info("Redis MMR cache SET. key={}, ttlSeconds={}", cacheKey, MMR_CACHE_TTL.getSeconds());
        } catch (Exception e) {
            log.warn("Redis MMR cache write failed. key={}, cause={}", cacheKey, e.getMessage());
        }
    }
}