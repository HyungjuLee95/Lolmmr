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
import org.springframework.web.client.RestTemplate;

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
        if (tier == null || tier.equals("UNRANKED") || tier.isEmpty())
            return 1000;

        Map<String, Integer> base = Map.of(
                "IRON", 500, "BRONZE", 700, "SILVER", 900, "GOLD", 1100,
                "PLATINUM", 1300, "EMERALD", 1500, "DIAMOND", 1700,
                "MASTER", 1900, "GRANDMASTER", 2100, "CHALLENGER", 2300);
        Map<String, Integer> offset = Map.of("IV", 0, "III", 50, "II", 100, "I", 150);

        return base.getOrDefault(tier.toUpperCase(), 1000) + offset.getOrDefault(rank.toUpperCase(), 0);
    }

    private <T> T riotGet(String url, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", apiKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<T> resp = restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
        return resp.getBody();
    }

    @Cacheable(value = "summonerInfo", key = "#gameName + '-' + #tagLine", cacheManager = "cacheManager")
    public SummonerDTO getSummonerInfo(String gameName, String tagLine) {
        String accountUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" + gameName + "/"
                + tagLine;
        Map<String, Object> accountResponse = riotGet(accountUrl, Map.class);
        if (accountResponse == null || accountResponse.get("puuid") == null) {
            throw new RuntimeException("해당 닉네임의 계정을 찾을 수 없습니다.");
        }

        String puuid = (String) accountResponse.get("puuid");
        String realName = (String) accountResponse.get("gameName");

        String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid;
        Map<String, Object> summonerMap = riotGet(summonerUrl, Map.class);

        SummonerDTO summoner = new SummonerDTO();
        summoner.setPuuid(puuid);
        summoner.setName(realName);

        if (summonerMap != null) {
            summoner.setId((String) summonerMap.get("id"));
            summoner.setSummonerLevel(((Number) summonerMap.get("summonerLevel")).intValue());
        }

        String leagueUrl = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid;

        Object[] leagueResponse = riotGet(leagueUrl, Object[].class);

        if (leagueResponse != null && leagueResponse.length > 0) {
            for (Object obj : leagueResponse) {
                Map<String, Object> data = (Map<String, Object>) obj;
                String queueType = (String) data.get("queueType");
                String tier = (String) data.get("tier");
                String rankStr = (String) data.get("rank");
                int lp = ((Number) data.get("leaguePoints")).intValue();

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

        List<MatchSummary> soloMatchDetails = Collections.emptyList();
        List<MatchSummary> flexMatchDetails = Collections.emptyList();

        if (needSolo && needFlex) {
            CompletableFuture<List<MatchSummary>> soloDetailsFuture = CompletableFuture
                    .supplyAsync(() -> riotMatchService.getMatchSummaries(puuid, soloMatchIds, 420), executorService);
            CompletableFuture<List<MatchSummary>> flexDetailsFuture = CompletableFuture
                    .supplyAsync(() -> riotMatchService.getMatchSummaries(puuid, flexMatchIds, 440), executorService);
            soloMatchDetails = soloDetailsFuture.join();
            flexMatchDetails = flexDetailsFuture.join();
        } else if (needSolo) {
            soloMatchDetails = riotMatchService.getMatchSummaries(puuid, soloMatchIds, 420);
        } else if (needFlex) {
            flexMatchDetails = riotMatchService.getMatchSummaries(puuid, flexMatchIds, 440);
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

        return result;
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
        if (currentSummoner.getSoloRank() != null && lastRecord.isPresent()
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
