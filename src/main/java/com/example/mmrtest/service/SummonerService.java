package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;
import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.entity.SummonerHistory;
import com.example.mmrtest.repository.SummonerHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.cache.annotation.Cacheable;

@Service
public class SummonerService {
    @Autowired
    private SummonerHistoryRepository historyRepository;

    @Autowired
    private ScoreEngine scoreEngine;

    @Value("${riot.api.key}")
    private String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    // 비동기 처리를 위한 스레드 풀 환경 설정
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 1. 티어/랭크를 숫자로 변환 (기준점 산출)
     */
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

    /**
     * 2. 소환사 기본 정보 및 티어 정보 조회
     */
    @Cacheable(value = "summonerInfo", key = "#gameName + '-' + #tagLine", cacheManager = "cacheManager")
    public SummonerDTO getSummonerInfo(String gameName, String tagLine) {
        // 1. Account-v1: puuid 획득 (생략 없음)
        String accountUrl = "https://asia.api.riotgames.com/riot/account/v1/accounts/by-riot-id/" + gameName + "/"
                + tagLine + "?api_key=" + apiKey;
        Map<String, Object> accountResponse = restTemplate.getForObject(accountUrl, Map.class);
        if (accountResponse == null || accountResponse.get("puuid") == null) {
            throw new RuntimeException("해당 닉네임의 계정을 찾을 수 없습니다.");
        }

        String puuid = (String) accountResponse.get("puuid");
        String realName = (String) accountResponse.get("gameName");

        // 2. Summoner-v4: 레벨 및 ID 획득
        String summonerUrl = "https://kr.api.riotgames.com/lol/summoner/v4/summoners/by-puuid/" + puuid + "?api_key="
                + apiKey;
        Map<String, Object> summonerMap = restTemplate.getForObject(summonerUrl, Map.class);

        SummonerDTO summoner = new SummonerDTO();
        summoner.setPuuid(puuid);
        summoner.setName(realName);

        if (summonerMap != null) {
            summoner.setId((String) summonerMap.get("id")); // 만약 null이어도 아래에서 puuid를 쓰면 안전함
            summoner.setSummonerLevel(((Number) summonerMap.get("summonerLevel")).intValue());
        }

        // [핵심 수정 부분] 3. League-v4: ID 대신 PUUID를 사용하여 리그 정보 조회
        String leagueUrl = "https://kr.api.riotgames.com/lol/league/v4/entries/by-puuid/" + puuid + "?api_key="
                + apiKey;

        Object[] leagueResponse = restTemplate.getForObject(leagueUrl, Object[].class);

        if (leagueResponse != null && leagueResponse.length > 0) {
            for (Object obj : leagueResponse) {
                Map<String, Object> data = (Map<String, Object>) obj;
                String queueType = (String) data.get("queueType");
                String tier = (String) data.get("tier");
                String rankStr = (String) data.get("rank");
                int lp = ((Number) data.get("leaguePoints")).intValue();

                if ("RANKED_SOLO_5x5".equals(queueType)) {
                    summoner.setSoloRank(new SummonerDTO.RankInfo(tier, rankStr, lp));
                    // 호환성 유지용 (차후 삭제)
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

    /**
     * 3. 매치 ID 리스트 조회 (큐별로 조회 가능, 기본 100판)
     */
    @Cacheable(value = "matchIds", key = "#puuid + '-' + #queueId", cacheManager = "cacheManager")
    public List<String> getMatchIds(String puuid, Integer queueId) {
        String url = "https://asia.api.riotgames.com/lol/match/v5/matches/by-puuid/" + puuid
                + "/ids?start=0&count=100&api_key=" + apiKey;
        if (queueId != null) {
            url += "&queue=" + queueId; // 420 (Solo), 440 (Flex)
        }
        String[] matchIds = restTemplate.getForObject(url, String[].class);
        return matchIds != null ? Arrays.asList(matchIds) : new ArrayList<>();
    }

    /**
     * 4. 매치 상세 정보 추출 (병렬 처리 지원)
     */
    public List<MatchSummary> getMatchSummaries(String puuid, List<String> matchIds) {
        List<CompletableFuture<MatchSummary>> futures = matchIds.stream()
                .map(matchId -> CompletableFuture.supplyAsync(() -> fetchMatchDetail(puuid, matchId), executorService))
                .collect(Collectors.toList());

        // Exception으로 null이 된 요소 필터링 후 반환
        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // 개별 매치 상세 정보 추출 (캐시 적용)
    @Cacheable(value = "matchDetail", key = "#matchId", cacheManager = "cacheManager")
    public MatchSummary fetchMatchDetail(String puuid, String matchId) {
        try {
            String url = "https://asia.api.riotgames.com/lol/match/v5/matches/" + matchId + "?api_key=" + apiKey;
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            Map<String, Object> info = (Map<String, Object>) response.get("info");
            List<Map<String, Object>> participants = (List<Map<String, Object>>) info.get("participants");

            for (Map<String, Object> p : participants) {
                if (p.get("puuid").equals(puuid)) {
                    int myTeamId = (int) p.get("teamId");

                    // 10인 리스트 (내 팀 5명 -> 상대 팀 5명 순서 정렬)
                    List<Map<String, Object>> sortedParts = participants.stream()
                            .sorted((p1, p2) -> {
                                int t1 = (int) p1.get("teamId");
                                int t2 = (int) p2.get("teamId");
                                if (t1 == myTeamId && t2 != myTeamId)
                                    return -1;
                                if (t1 != myTeamId && t2 == myTeamId)
                                    return 1;
                                return 0;
                            }).collect(Collectors.toList());

                    List<String> teamMembers = sortedParts.stream()
                            .map(part -> part.get("riotIdGameName") + "#" + part.get("riotIdTagline"))
                            .collect(Collectors.toList());

                    List<String> teamChamps = sortedParts.stream().map(part -> (String) part.get("championName"))
                            .collect(Collectors.toList());

                    // CS 및 골드, 시간 데이터 추출
                    int totalCs = (int) p.get("totalMinionsKilled") + (int) p.get("neutralMinionsKilled");
                    int goldEarned = (int) p.get("goldEarned");
                    long gameEndTimeStamp = (long) info.get("gameEndTimestamp");
                    int duration = ((Number) info.get("gameDuration")).intValue() / 60;

                    // 아이템 리스트 추출
                    List<Integer> items = Arrays.asList(
                            (int) p.get("item0"), (int) p.get("item1"), (int) p.get("item2"),
                            (int) p.get("item3"), (int) p.get("item4"), (int) p.get("item5"), (int) p.get("item6"));

                    // 룬 정보 추출
                    Map<String, Object> perks = (Map<String, Object>) p.get("perks");
                    List<Map<String, Object>> styles = (List<Map<String, Object>>) perks.get("styles");
                    int mainRuneId = (int) ((List<Map<String, Object>>) styles.get(0).get("selections")).get(0)
                            .get("perk");
                    int subRuneId = (int) styles.get(1).get("style");

                    return new MatchSummary(
                            (boolean) p.get("win"), (int) p.get("kills"), (int) p.get("deaths"),
                            (int) p.get("assists"),
                            (String) p.get("championName"), items, teamMembers, teamChamps, duration,
                            (int) p.get("summoner1Id"), (int) p.get("summoner2Id"), mainRuneId, subRuneId,
                            totalCs, goldEarned, gameEndTimeStamp,
                            ((int) p.get("kills") + (int) p.get("assists")) - (int) p.get("deaths"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 실패시 null 반환 (상단 컴플리터블퓨처에서 필터링)
    }

    /**
     * 5. 통합 MMR 분석 로직 (컨트롤러에서 호출)
     */
    public Map<String, Object> getMmrAnalysis(String gameName, String tagLine) {
        SummonerDTO currentSummoner = getSummonerInfo(gameName, tagLine);
        String puuid = currentSummoner.getPuuid();

        // 1. 솔랭 및 자랭 Match ID 병렬 조회 (최대 100판)
        CompletableFuture<List<String>> soloIdsFuture = CompletableFuture.supplyAsync(() -> getMatchIds(puuid, 420),
                executorService);
        CompletableFuture<List<String>> flexIdsFuture = CompletableFuture.supplyAsync(() -> getMatchIds(puuid, 440),
                executorService);

        List<String> soloMatchIds = soloIdsFuture.join();
        List<String> flexMatchIds = flexIdsFuture.join();

        // 2. 솔랭 및 자랭 상세 정보 병렬 추출 (비동기로 최대 각 100판, 총 200판 처리 가능)
        CompletableFuture<List<MatchSummary>> soloDetailsFuture = CompletableFuture
                .supplyAsync(() -> getMatchSummaries(puuid, soloMatchIds), executorService);
        CompletableFuture<List<MatchSummary>> flexDetailsFuture = CompletableFuture
                .supplyAsync(() -> getMatchSummaries(puuid, flexMatchIds), executorService);

        List<MatchSummary> soloMatchDetails = soloDetailsFuture.join();
        List<MatchSummary> flexMatchDetails = flexDetailsFuture.join();

        // 3. 점수 엔진을 통한 누적 점수 및 변동 히스토리 계산 (기본 1000점 시작)
        ScoreResult soloScoreResult = scoreEngine.calculateScore(soloMatchDetails, 1000);
        ScoreResult flexScoreResult = scoreEngine.calculateScore(flexMatchDetails, 1000);

        // 기존 검색 기록과 비교하여 LP 변동폭 계산 로직
        // 솔랭을 기준으로 우선적으로 설정 혹은 별도 분리 저장(향후 Entity 분리 시 수정 권장)
        Optional<SummonerHistory> lastRecord = historyRepository.findFirstByPuuidOrderBySearchTimeDesc(puuid);
        int soloLpChange = 0;
        int flexLpChange = 0; // 향후 유연하게 자랭 LP도 별도로 기록하게 DB Entity를 바꿀 필요가 있음.
        if (lastRecord.isPresent() && lastRecord.get().getTier().equals(currentSummoner.getSoloRank().getTier())) {
            soloLpChange = currentSummoner.getSoloRank().getLeaguePoints() - lastRecord.get().getLeaguePoints();
        }

        // 현재 기록 DB 저장 (솔랭 기준 저장, 필요 시 엔티티 변경 후 분리)
        SummonerHistory history = new SummonerHistory();
        history.setPuuid(puuid);
        history.setTier(currentSummoner.getSoloRank().getTier());
        history.setRank(currentSummoner.getSoloRank().getRank());
        history.setLeaguePoints(currentSummoner.getSoloRank().getLeaguePoints());
        history.setSearchTime(LocalDateTime.now());
        historyRepository.save(history);

        // 최종 맵 구성 (queues 래퍼 구조로 바로 반환하게 설계할 수 있게 DTO 통 구조 정리 지원용 필드 마련)
        Map<String, Object> result = new HashMap<>();
        result.put("summoner", currentSummoner);

        // 기존과 동일하게 root에 내려가지만, Controller 단에서 queues 내부로 분산시킬 데이터
        result.put("soloMatchDetails", soloMatchDetails);
        result.put("flexMatchDetails", flexMatchDetails);
        result.put("soloLpChange", soloLpChange);
        result.put("flexLpChange", flexLpChange);
        result.put("soloScoreResult", soloScoreResult);
        result.put("flexScoreResult", flexScoreResult);

        return result;
    }
}