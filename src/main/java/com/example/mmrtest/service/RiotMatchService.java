package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchResultType;
import com.example.mmrtest.dto.MatchSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class RiotMatchService {

    private static final String ASIA_BASE_URL = "https://asia.api.riotgames.com";
    private static final int SOLO_RANK_QUEUE_ID = 420;
    private static final int FLEX_RANK_QUEUE_ID = 440;
    private static final int DEFAULT_MATCH_COUNT = 10;

    private final RestTemplate restTemplate;

    @Value("${riot.api.key}")
    private String riotApiKey;

    public RiotMatchService() {
        this.restTemplate = new RestTemplate();
    }

    public List<MatchSummary> fetchRecentRankedMatches(String puuid, int count) {
        return fetchRecentRankedMatches(puuid, count, null);
    }

    public List<MatchSummary> fetchRecentRankedMatches(String puuid, int count, Integer queueFilter) {
        List<String> matchIds = fetchMatchIds(puuid, count);
        if (matchIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<MatchSummary> results = new ArrayList<>();

        for (String matchId : matchIds) {
            MatchSummary summary = fetchMatchDetail(puuid, matchId);
            if (summary == null) {
                continue;
            }

            if (queueFilter != null && summary.getQueueId() != queueFilter) {
                continue;
            }

            if (summary.getQueueId() != SOLO_RANK_QUEUE_ID && summary.getQueueId() != FLEX_RANK_QUEUE_ID) {
                continue;
            }

            results.add(summary);

            if (results.size() >= count) {
                break;
            }
        }

        return results;
    }

    public List<String> getMatchIds(String puuid, int queueId) {
        return getMatchIds(puuid, queueId, DEFAULT_MATCH_COUNT);
    }

    public List<String> getMatchIds(String puuid, int queueId, int count) {
        if (puuid == null || puuid.isBlank()) {
            return Collections.emptyList();
        }

        if (queueId != SOLO_RANK_QUEUE_ID && queueId != FLEX_RANK_QUEUE_ID) {
            return Collections.emptyList();
        }

        int safeCount = count <= 0 ? DEFAULT_MATCH_COUNT : count;

        String url = ASIA_BASE_URL
                + "/lol/match/v5/matches/by-puuid/"
                + puuid
                + "/ids?type=ranked&queue="
                + queueId
                + "&start=0&count="
                + safeCount;

        List<String> ids = riotGet(url, new ParameterizedTypeReference<List<String>>() {});
        return ids == null ? Collections.emptyList() : ids;
    }

    public List<MatchSummary> getMatchSummaries(String puuid, List<String> matchIds, int queueId) {
        if (puuid == null || puuid.isBlank() || matchIds == null || matchIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<MatchSummary> results = new ArrayList<>();
        for (String matchId : matchIds) {
            MatchSummary summary = fetchMatchDetail(puuid, matchId);
            if (summary == null) {
                continue;
            }

            if (queueId > 0 && summary.getQueueId() != queueId) {
                continue;
            }

            if (summary.getQueueId() != SOLO_RANK_QUEUE_ID && summary.getQueueId() != FLEX_RANK_QUEUE_ID) {
                continue;
            }

            results.add(summary);
        }

        return results;
    }

    @Cacheable(value = "matchIds", key = "#puuid + ':' + #count", cacheManager = "cacheManager")
    public List<String> fetchMatchIds(String puuid, int count) {
        String url = ASIA_BASE_URL
                + "/lol/match/v5/matches/by-puuid/"
                + puuid
                + "/ids?type=ranked&start=0&count="
                + count;

        List<String> ids = riotGet(url, new ParameterizedTypeReference<List<String>>() {});
        return ids == null ? Collections.emptyList() : ids;
    }

    @Cacheable(value = "matchDetail", key = "#matchId + ':' + #puuid", cacheManager = "cacheManager")
    public MatchSummary fetchMatchDetail(String puuid, String matchId) {
        Map<String, Object> rawMatch = fetchMatchRaw(matchId);
        if (rawMatch == null) {
            return null;
        }
        return buildMatchSummary(puuid, rawMatch);
    }

    @Cacheable(value = "matchRaw", key = "#matchId", cacheManager = "cacheManager")
    public Map<String, Object> fetchMatchRaw(String matchId) {
        String url = ASIA_BASE_URL + "/lol/match/v5/matches/" + matchId;
        return riotGet(url, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    @Cacheable(value = "matchTimelineRaw", key = "#matchId", cacheManager = "cacheManager")
    public Map<String, Object> fetchMatchTimelineRaw(String matchId) {
        String url = ASIA_BASE_URL + "/lol/match/v5/matches/" + matchId + "/timeline";
        return riotGet(url, new ParameterizedTypeReference<Map<String, Object>>() {});
    }

    private MatchSummary buildMatchSummary(String puuid, Map<String, Object> rawMatch) {
        if (rawMatch == null) {
            return null;
        }

        Map<String, Object> info = asMap(rawMatch.get("info"));
        if (info == null) {
            return null;
        }

        List<Map<String, Object>> participants = asListOfMaps(info.get("participants"));
        if (participants.isEmpty()) {
            return null;
        }

        Map<String, Object> me = null;
        for (Map<String, Object> participant : participants) {
            if (puuid.equals(getString(participant, "puuid", ""))) {
                me = participant;
                break;
            }
        }

        if (me == null) {
            return null;
        }

        MatchSummary summary = new MatchSummary();

        String matchId = getString(rawMatch, "metadata.matchId", "");
        if (matchId.isBlank()) {
            Map<String, Object> metadata = asMap(rawMatch.get("metadata"));
            matchId = metadata == null ? "" : getString(metadata, "matchId", "");
        }

        int gameDurationSeconds = getInt(info, "gameDuration", 0);
        if (gameDurationSeconds <= 0) {
            gameDurationSeconds = getInt(info, "gameLength", 0);
        }

        int gameDurationMinutes = Math.max(1, gameDurationSeconds / 60);

        boolean win = getBoolean(me, "win", false);
        boolean gameEndedInEarlySurrender = getBoolean(info, "gameEndedInEarlySurrender", false);
        boolean earlyRemakeByDuration = gameDurationSeconds > 0 && gameDurationSeconds < 240;

        MatchResultType resultType;
        if (gameEndedInEarlySurrender || earlyRemakeByDuration) {
            resultType = MatchResultType.REMAKE;
        } else if (win) {
            resultType = MatchResultType.WIN;
        } else {
            resultType = MatchResultType.LOSS;
        }

        boolean remake = resultType == MatchResultType.REMAKE;
        boolean invalid = resultType == MatchResultType.INVALID;
        boolean countedGame = resultType == MatchResultType.WIN || resultType == MatchResultType.LOSS;
        boolean loss = resultType == MatchResultType.LOSS;

        summary.setMatchId(matchId);
        summary.setResultType(resultType);
        summary.setKills(getInt(me, "kills", 0));
        summary.setDeaths(getInt(me, "deaths", 0));
        summary.setAssists(getInt(me, "assists", 0));
        summary.setChampionName(getString(me, "championName", "Unknown"));
        summary.setItems(List.of(
                getInt(me, "item0", 0),
                getInt(me, "item1", 0),
                getInt(me, "item2", 0),
                getInt(me, "item3", 0),
                getInt(me, "item4", 0),
                getInt(me, "item5", 0),
                getInt(me, "item6", 0)
        ));
        summary.setTeamMembers(extractTeamMembers(participants));
        summary.setTeamChamps(extractTeamChamps(participants));
        summary.setGameDurationMinutes(gameDurationMinutes);
        summary.setSpell1Id(getInt(me, "summoner1Id", 0));
        summary.setSpell2Id(getInt(me, "summoner2Id", 0));
        summary.setMainRuneId(extractMainRuneId(me));
        summary.setSubRuneId(extractSubRuneId(me));
        summary.setTotalCs(getInt(me, "totalMinionsKilled", 0) + getInt(me, "neutralMinionsKilled", 0));
        summary.setGoldEarned(getInt(me, "goldEarned", 0));
        summary.setQueueId(getInt(info, "queueId", 0));
        summary.setGameEndTimeStamp(getLong(info, "gameEndTimestamp", 0L));
        summary.setPerformanceScore(0);
        summary.setTeamPosition(resolveTeamPosition(me));
        summary.setInvalid(invalid);
        summary.setCountedGame(countedGame);
        summary.setRemake(remake);
        summary.setWin(win && countedGame);
        summary.setDisplayResult(resolveDisplayResult(resultType));
        summary.setLoss(loss);

        return summary;
    }

    private List<String> extractTeamMembers(List<Map<String, Object>> participants) {
        List<String> members = new ArrayList<>();
        for (Map<String, Object> participant : participants) {
            members.add(buildRiotId(participant));
        }
        return members;
    }

    private List<String> extractTeamChamps(List<Map<String, Object>> participants) {
        List<String> champs = new ArrayList<>();
        for (Map<String, Object> participant : participants) {
            champs.add(getString(participant, "championName", "Unknown"));
        }
        return champs;
    }

    private String buildRiotId(Map<String, Object> participant) {
        String gameName = getString(participant, "riotIdGameName", "");
        String tagLine = getString(participant, "riotIdTagline", "");

        if (!gameName.isBlank() && !tagLine.isBlank()) {
            return gameName + "#" + tagLine;
        }
        if (!gameName.isBlank()) {
            return gameName;
        }

        String summonerName = getString(participant, "summonerName", "");
        return summonerName.isBlank() ? "Unknown Player" : summonerName;
    }

    private int extractMainRuneId(Map<String, Object> participant) {
        Map<String, Object> perks = asMap(participant.get("perks"));
        if (perks == null) return 0;

        List<Map<String, Object>> styles = asListOfMaps(perks.get("styles"));
        if (styles.isEmpty()) return 0;

        Map<String, Object> primaryStyle = styles.get(0);
        List<Map<String, Object>> selections = asListOfMaps(primaryStyle.get("selections"));
        if (selections.isEmpty()) return 0;

        return getInt(selections.get(0), "perk", 0);
    }

    private int extractSubRuneId(Map<String, Object> participant) {
        Map<String, Object> perks = asMap(participant.get("perks"));
        if (perks == null) return 0;

        List<Map<String, Object>> styles = asListOfMaps(perks.get("styles"));
        if (styles.size() < 2) return 0;

        return getInt(styles.get(1), "style", 0);
    }

    private String resolveTeamPosition(Map<String, Object> participant) {
        String teamPosition = getString(participant, "teamPosition", "");
        if (!teamPosition.isBlank()) {
            return teamPosition;
        }

        String individualPosition = getString(participant, "individualPosition", "");
        if (!individualPosition.isBlank()) {
            return individualPosition;
        }

        String lane = getString(participant, "lane", "");
        if (!lane.isBlank()) {
            return lane;
        }

        String role = getString(participant, "role", "");
        return role.isBlank() ? "UNKNOWN" : role;
    }

    private String resolveDisplayResult(MatchResultType resultType) {
        return switch (resultType) {
            case WIN -> "승리";
            case LOSS -> "패배";
            case REMAKE -> "다시하기";
            case INVALID -> "분석 제외";
        };
    }

    private <T> T riotGet(String url, ParameterizedTypeReference<T> typeReference) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", riotApiKey);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<T> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                requestEntity,
                typeReference
        );

        return response.getBody();
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