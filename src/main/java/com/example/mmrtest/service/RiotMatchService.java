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
    private static final long FIFTEEN_MINUTES_MS = 15L * 60_000L;
    private static final long OBJECTIVE_WINDOW_MS = 90_000L;

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
        Map<String, Object> timelineRaw = fetchMatchTimelineRaw(matchId);
        return buildMatchSummary(puuid, rawMatch, timelineRaw);
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

    private MatchSummary buildMatchSummary(String puuid, Map<String, Object> rawMatch, Map<String, Object> timelineRaw) {
        if (rawMatch == null) {
            return null;
        }

        Map<String, Object> info = asMap(rawMatch.get("info"));
        Map<String, Object> metadata = asMap(rawMatch.get("metadata"));
        if (info == null) {
            return null;
        }

        List<Map<String, Object>> participants = asListOfMaps(info.get("participants"));
        if (participants.isEmpty()) {
            return null;
        }

        Map<String, Object> me = findParticipantByPuuid(participants, puuid);
        if (me == null) {
            return null;
        }

        MatchSummary summary = new MatchSummary();

        String matchId = metadata == null ? "" : getString(metadata, "matchId", "");
        int gameDurationSeconds = getInt(info, "gameDuration", 0);
        if (gameDurationSeconds <= 0) {
            gameDurationSeconds = (int) Math.round(getDouble(info, "gameLength", 0.0));
        }
        int gameDurationMinutes = Math.max(1, (int) Math.round(gameDurationSeconds / 60.0));

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

        int myTeamId = getInt(me, "teamId", 0);
        int participantId = getInt(me, "participantId", 0);
        String resolvedPosition = resolveTeamPosition(me);
        Map<String, Object> laneOpponent = findLaneOpponent(me, participants);

        TeamAggregate myTeam = buildTeamAggregate(participants, myTeamId);
        TimelineDerivedMetrics timelineMetrics = buildTimelineDerivedMetrics(
                timelineRaw,
                participantId,
                myTeamId,
                laneOpponent == null ? 0 : getInt(laneOpponent, "participantId", 0),
                gameDurationSeconds,
                remake
        );

        int totalCs = getTotalCs(me);
        int goldEarned = getInt(me, "goldEarned", 0);
        int damageToChampions = getInt(me, "totalDamageDealtToChampions", 0);
        int teamKills = myTeam.kills;
        int teamGold = myTeam.gold;
        int teamDamage = myTeam.damage;

        double killParticipation = teamKills == 0
                ? 0.0
                : (getInt(me, "kills", 0) + getInt(me, "assists", 0)) / (double) teamKills;

        double damageShare = teamDamage == 0 ? 0.0 : damageToChampions / (double) teamDamage;
        double goldShare = teamGold == 0 ? 0.0 : goldEarned / (double) teamGold;
        double damageConversion = goldShare <= 0.0 ? 0.0 : damageShare / goldShare;

        double durationMinutesSafe = Math.max(gameDurationMinutes, 1);
        double visionPerMinute = getInt(me, "visionScore", 0) / durationMinutesSafe;
        double goldPerMinute = goldEarned / durationMinutesSafe;
        double csPerMinute = totalCs / durationMinutesSafe;

        double totalTimeSpentDead = getInt(me, "totalTimeSpentDead", 0);
        double playableSeconds = Math.max(gameDurationSeconds, 1);
        double timeAliveRatio = clampDouble(1.0 - (totalTimeSpentDead / playableSeconds), 0.0, 1.0);

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
        summary.setTeamMembers(extractTeamMembers(participants, myTeamId));
        summary.setTeamChamps(extractTeamChamps(participants, myTeamId));
        summary.setGameDurationMinutes(gameDurationMinutes);
        summary.setSpell1Id(getInt(me, "summoner1Id", 0));
        summary.setSpell2Id(getInt(me, "summoner2Id", 0));
        summary.setMainRuneId(extractMainRuneId(me));
        summary.setSubRuneId(extractSubRuneId(me));
        summary.setTotalCs(totalCs);
        summary.setGoldEarned(goldEarned);
        summary.setQueueId(getInt(info, "queueId", 0));
        summary.setGameEndTimeStamp(resolveGameEndTimestamp(info));
        summary.setPerformanceScore(0);
        summary.setTeamPosition(resolvedPosition);
        summary.setInvalid(invalid);
        summary.setCountedGame(countedGame);
        summary.setRemake(remake);
        summary.setWin(win && countedGame);
        summary.setDisplayResult(resolveDisplayResult(resultType));
        summary.setLoss(loss);

        summary.setParticipantId(participantId);
        summary.setTeamId(myTeamId);
        summary.setRiotId(buildRiotId(me));
        summary.setChampionLevel(getInt(me, "champLevel", 1));
        summary.setVisionScore(getInt(me, "visionScore", 0));
        summary.setControlWardsPlaced(getInt(me, "detectorWardsPlaced", 0));
        summary.setWardsPlaced(getInt(me, "wardsPlaced", 0));
        summary.setWardsKilled(getInt(me, "wardsKilled", 0));
        summary.setTotalTimeSpentDead((int) Math.round(totalTimeSpentDead));
        summary.setDamageToChampions(damageToChampions);
        summary.setDamageToObjectives(getInt(me, "damageDealtToObjectives", 0));
        summary.setDamageToTurrets(getInt(me, "damageDealtToTurrets", 0));
        summary.setTeamKills(teamKills);
        summary.setTeamGoldEarned(teamGold);
        summary.setTeamDamageToChampions(teamDamage);
        summary.setGoldDiff15(timelineMetrics.goldDiff15);
        summary.setCsDiff15(timelineMetrics.csDiff15);
        summary.setXpDiff15(timelineMetrics.xpDiff15);
        summary.setKillParticipation(round4(killParticipation));
        summary.setDamageShare(round4(damageShare));
        summary.setDamageConversion(round4(damageConversion));
        summary.setVisionPerMinute(round4(visionPerMinute));
        summary.setGoldPerMinute(round4(goldPerMinute));
        summary.setCsPerMinute(round4(csPerMinute));
        summary.setTimeAliveRatio(round4(timeAliveRatio));
        summary.setObjectiveParticipationScore(timelineMetrics.objectiveParticipationScore);
        summary.setThrowDeathPenalty(timelineMetrics.throwDeathPenalty);
        summary.setLeaver(resolveLeaver(me, gameDurationSeconds, remake));
        summary.setBaseDelta(0);
        summary.setPerformanceDelta(0);
        summary.setFinalDelta(0);
        summary.setPerfIndex(0.0);
        summary.setGrowthScore(0.0);
        summary.setTeamplayScore(0.0);
        summary.setEfficiencyScore(0.0);
        summary.setSurvivalScore(0.0);
        summary.setScoreTier("");

        return summary;
    }

    private TeamAggregate buildTeamAggregate(List<Map<String, Object>> participants, int myTeamId) {
        TeamAggregate aggregate = new TeamAggregate();
        for (Map<String, Object> participant : participants) {
            if (getInt(participant, "teamId", 0) != myTeamId) {
                continue;
            }
            aggregate.kills += getInt(participant, "kills", 0);
            aggregate.gold += getInt(participant, "goldEarned", 0);
            aggregate.damage += getInt(participant, "totalDamageDealtToChampions", 0);
        }
        return aggregate;
    }

    private TimelineDerivedMetrics buildTimelineDerivedMetrics(
            Map<String, Object> timelineRaw,
            int myParticipantId,
            int myTeamId,
            int opponentParticipantId,
            int gameDurationSeconds,
            boolean remake
    ) {
        TimelineDerivedMetrics metrics = new TimelineDerivedMetrics();
        if (timelineRaw == null || myParticipantId == 0 || remake) {
            return metrics;
        }

        Map<String, Object> info = asMap(timelineRaw.get("info"));
        if (info == null) {
            return metrics;
        }

        List<Map<String, Object>> frames = asListOfMaps(info.get("frames"));
        if (frames.isEmpty()) {
            return metrics;
        }

        Map<String, Object> frame15 = findFrameAtOrBefore(frames, FIFTEEN_MINUTES_MS);
        if (frame15 != null) {
            Map<String, Object> myFrame = getParticipantFrame(frame15, myParticipantId);
            Map<String, Object> oppFrame = opponentParticipantId == 0 ? null : getParticipantFrame(frame15, opponentParticipantId);
            if (myFrame != null && oppFrame != null) {
                metrics.goldDiff15 = getInt(myFrame, "totalGold", 0) - getInt(oppFrame, "totalGold", 0);
                metrics.csDiff15 = (getInt(myFrame, "minionsKilled", 0) + getInt(myFrame, "jungleMinionsKilled", 0))
                        - (getInt(oppFrame, "minionsKilled", 0) + getInt(oppFrame, "jungleMinionsKilled", 0));
                metrics.xpDiff15 = getInt(myFrame, "xp", 0) - getInt(oppFrame, "xp", 0);
            }
        }

        List<Long> majorObjectiveTimestamps = collectMajorObjectiveTimestamps(frames);
        metrics.objectiveParticipationScore = collectObjectiveParticipationScore(frames, myParticipantId, myTeamId);
        metrics.throwDeathPenalty = collectThrowDeathPenalty(frames, myParticipantId, gameDurationSeconds, majorObjectiveTimestamps);
        return metrics;
    }

    private List<Long> collectMajorObjectiveTimestamps(List<Map<String, Object>> frames) {
        List<Long> result = new ArrayList<>();
        for (Map<String, Object> frame : frames) {
            List<Map<String, Object>> events = asListOfMaps(frame.get("events"));
            for (Map<String, Object> event : events) {
                String type = getString(event, "type", "");
                if (!"ELITE_MONSTER_KILL".equals(type)) {
                    continue;
                }
                String monsterType = getString(event, "monsterType", "");
                String monsterSubType = getString(event, "monsterSubType", "");
                if ("BARON_NASHOR".equals(monsterType)
                        || "RIFTHERALD".equals(monsterType)
                        || "DRAGON".equals(monsterType)
                        || "ELDER_DRAGON".equals(monsterSubType)) {
                    result.add(getLong(event, "timestamp", 0L));
                }
            }
        }
        return result;
    }

    private int collectObjectiveParticipationScore(
            List<Map<String, Object>> frames,
            int participantId,
            int myTeamId
    ) {
        int score = 0;
        for (Map<String, Object> frame : frames) {
            List<Map<String, Object>> events = asListOfMaps(frame.get("events"));
            for (Map<String, Object> event : events) {
                String type = getString(event, "type", "");

                if ("ELITE_MONSTER_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));
                    int eventTeamId = resolveEventTeamId(event, killerId);
                    if (eventTeamId != myTeamId) {
                        continue;
                    }
                    int objectiveScore = objectiveMonsterScore(
                            getString(event, "monsterType", ""),
                            getString(event, "monsterSubType", "")
                    );
                    if (killerId == participantId) {
                        score += objectiveScore;
                    } else if (assistingIds.contains(participantId)) {
                        score += Math.max(1, objectiveScore / 2);
                    }
                }

                if ("BUILDING_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));
                    int eventTeamId = resolveEventTeamId(event, killerId);
                    if (eventTeamId != myTeamId) {
                        continue;
                    }
                    String buildingType = getString(event, "buildingType", "");
                    if (!"TOWER_BUILDING".equals(buildingType) && !"INHIBITOR_BUILDING".equals(buildingType)) {
                        continue;
                    }
                    int buildingScore = "INHIBITOR_BUILDING".equals(buildingType) ? 3 : 2;
                    if (killerId == participantId) {
                        score += buildingScore;
                    } else if (assistingIds.contains(participantId)) {
                        score += 1;
                    }
                }
            }
        }
        return score;
    }

    private int collectThrowDeathPenalty(
            List<Map<String, Object>> frames,
            int participantId,
            int gameDurationSeconds,
            List<Long> majorObjectiveTimestamps
    ) {
        int penalty = 0;
        for (Map<String, Object> frame : frames) {
            List<Map<String, Object>> events = asListOfMaps(frame.get("events"));
            for (Map<String, Object> event : events) {
                if (!"CHAMPION_KILL".equals(getString(event, "type", ""))) {
                    continue;
                }
                if (getInt(event, "victimId", 0) != participantId) {
                    continue;
                }

                long timestamp = getLong(event, "timestamp", 0L);
                int minute = (int) (timestamp / 60_000L);
                int timeWeight = deathTimeWeight(minute, gameDurationSeconds);
                int objectiveWeight = objectiveDeathWeight(timestamp, majorObjectiveTimestamps);
                int isolationWeight = isolationDeathWeight(event);

                penalty += timeWeight + objectiveWeight + isolationWeight;
            }
        }
        return penalty;
    }

    private int deathTimeWeight(int minute, int gameDurationSeconds) {
        int gameMinutes = Math.max(1, (int) Math.round(gameDurationSeconds / 60.0));
        if (minute >= Math.max(28, gameMinutes - 5)) {
            return 4;
        }
        if (minute >= 20) {
            return 3;
        }
        if (minute >= 14) {
            return 2;
        }
        return 1;
    }

    private int objectiveDeathWeight(long deathTimestamp, List<Long> majorObjectiveTimestamps) {
        for (Long objectiveTimestamp : majorObjectiveTimestamps) {
            if (objectiveTimestamp == null) {
                continue;
            }
            long diff = objectiveTimestamp - deathTimestamp;
            if (diff < 0 || diff > OBJECTIVE_WINDOW_MS) {
                continue;
            }
            return 3;
        }
        return 0;
    }

    private int isolationDeathWeight(Map<String, Object> event) {
        List<Integer> assistingEnemyIds = getIntList(event.get("assistingParticipantIds"));
        if (assistingEnemyIds.isEmpty()) {
            return 2;
        }
        if (assistingEnemyIds.size() == 1) {
            return 1;
        }
        return 0;
    }

    private List<String> extractTeamMembers(List<Map<String, Object>> participants, int myTeamId) {
        List<String> members = new ArrayList<>();
        for (Map<String, Object> participant : participants) {
            if (getInt(participant, "teamId", 0) == myTeamId) {
                members.add(buildRiotId(participant));
            }
        }
        return members;
    }

    private List<String> extractTeamChamps(List<Map<String, Object>> participants, int myTeamId) {
        List<String> champs = new ArrayList<>();
        for (Map<String, Object> participant : participants) {
            if (getInt(participant, "teamId", 0) == myTeamId) {
                champs.add(getString(participant, "championName", "Unknown"));
            }
        }
        return champs;
    }

    private Map<String, Object> findParticipantByPuuid(List<Map<String, Object>> participants, String puuid) {
        for (Map<String, Object> participant : participants) {
            if (puuid.equals(getString(participant, "puuid", ""))) {
                return participant;
            }
        }
        return null;
    }

    private Map<String, Object> findLaneOpponent(Map<String, Object> me, List<Map<String, Object>> participants) {
        if (me == null) {
            return null;
        }

        int myTeamId = getInt(me, "teamId", 0);
        String myPosition = resolveTeamPosition(me);
        Map<String, Object> fallback = null;

        for (Map<String, Object> participant : participants) {
            if (getInt(participant, "teamId", 0) == myTeamId) {
                continue;
            }

            if (fallback == null) {
                fallback = participant;
            }

            String opponentPosition = resolveTeamPosition(participant);
            if (myPosition.equals(opponentPosition)) {
                return participant;
            }
        }

        return fallback;
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
        String individualPosition = getString(participant, "individualPosition", "");
        if (isUsablePosition(individualPosition)) {
            return normalizePosition(individualPosition);
        }

        String teamPosition = getString(participant, "teamPosition", "");
        if (isUsablePosition(teamPosition)) {
            return normalizePosition(teamPosition);
        }

        String lane = getString(participant, "lane", "");
        if (isUsablePosition(lane)) {
            return normalizePosition(lane);
        }

        String role = getString(participant, "role", "");
        return role.isBlank() ? "UNKNOWN" : normalizePosition(role);
    }

    private boolean isUsablePosition(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String upper = value.trim().toUpperCase();
        return !upper.equals("NONE")
                && !upper.equals("INVALID")
                && !upper.equals("UNKNOWN");
    }

    private String normalizePosition(String raw) {
        if (raw == null || raw.isBlank()) {
            return "UNKNOWN";
        }
        String upper = raw.trim().toUpperCase();
        return switch (upper) {
            case "MID" -> "MIDDLE";
            case "BOT", "ADC" -> "BOTTOM";
            case "SUP" -> "UTILITY";
            default -> upper;
        };
    }

    private String resolveDisplayResult(MatchResultType resultType) {
        return switch (resultType) {
            case WIN -> "승리";
            case LOSS -> "패배";
            case REMAKE -> "다시하기";
            case INVALID -> "분석 제외";
        };
    }

    private boolean resolveLeaver(Map<String, Object> participant, int gameDurationSeconds, boolean remake) {
        if (participant == null || remake || gameDurationSeconds <= 0) {
            return false;
        }

        int timePlayed = getInt(participant, "timePlayed", gameDurationSeconds);
        if (timePlayed > 0 && timePlayed < gameDurationSeconds * 0.7) {
            return true;
        }

        return getBoolean(participant, "leaver", false);
    }

    private long resolveGameEndTimestamp(Map<String, Object> info) {
        long gameEndTimestamp = getLong(info, "gameEndTimestamp", 0L);
        if (gameEndTimestamp > 0) {
            return gameEndTimestamp;
        }

        long gameCreation = getLong(info, "gameCreation", 0L);
        long gameDuration = getLong(info, "gameDuration", 0L);
        if (gameCreation > 0 && gameDuration > 0) {
            return gameCreation + (gameDuration * 1000L);
        }

        return gameCreation;
    }

    private int getTotalCs(Map<String, Object> participant) {
        return getInt(participant, "totalMinionsKilled", 0)
                + getInt(participant, "neutralMinionsKilled", 0);
    }

    private int resolveEventTeamId(Map<String, Object> event, int killerId) {
        int teamId = getInt(event, "teamId", 0);
        if (teamId == 100 || teamId == 200) {
            return teamId;
        }

        if (killerId >= 1 && killerId <= 5) {
            return 100;
        }
        if (killerId >= 6 && killerId <= 10) {
            return 200;
        }

        return 0;
    }

    private int objectiveMonsterScore(String monsterType, String monsterSubType) {
        if ("ELDER_DRAGON".equals(monsterSubType)) {
            return 7;
        }
        if (monsterType == null) {
            return 1;
        }

        return switch (monsterType) {
            case "DRAGON" -> 3;
            case "RIFTHERALD" -> 4;
            case "BARON_NASHOR" -> 6;
            case "HORDE" -> 4;
            default -> 2;
        };
    }

    private Map<String, Object> findFrameAtOrBefore(List<Map<String, Object>> frames, long targetTimestamp) {
        Map<String, Object> selected = null;

        for (Map<String, Object> frame : frames) {
            long timestamp = getLong(frame, "timestamp", 0L);
            if (timestamp <= targetTimestamp) {
                selected = frame;
            } else {
                break;
            }
        }

        if (selected == null && !frames.isEmpty()) {
            return frames.get(0);
        }

        return selected;
    }

    private Map<String, Object> getParticipantFrame(Map<String, Object> frame, int participantId) {
        Map<String, Object> participantFrames = asMap(frame.get("participantFrames"));
        if (participantFrames == null || participantFrames.isEmpty()) {
            return null;
        }

        Object byStringKey = participantFrames.get(String.valueOf(participantId));
        if (byStringKey != null) {
            return asMap(byStringKey);
        }

        Object byNumericKey = participantFrames.get(participantId);
        if (byNumericKey != null) {
            return asMap(byNumericKey);
        }

        return null;
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

    private List<Integer> getIntList(Object value) {
        if (!(value instanceof List<?> list)) {
            return Collections.emptyList();
        }

        List<Integer> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Number number) {
                result.add(number.intValue());
            }
        }
        return result;
    }

    private String getString(Map<String, Object> map, String key, String defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private long getLong(Map<String, Object> map, String key, long defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private double getDouble(Map<String, Object> map, String key, double defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return defaultValue;
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private static class TeamAggregate {
        private int kills;
        private int gold;
        private int damage;
    }

    private static class TimelineDerivedMetrics {
        private int goldDiff15;
        private int csDiff15;
        private int xpDiff15;
        private int objectiveParticipationScore;
        private int throwDeathPenalty;
    }
}