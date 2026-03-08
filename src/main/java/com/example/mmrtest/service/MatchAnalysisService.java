package com.example.mmrtest.service;

import com.example.mmrtest.dto.CoachingComment;
import com.example.mmrtest.dto.LaneOpponentComparison;
import com.example.mmrtest.dto.MatchAnalysisDetail;
import com.example.mmrtest.dto.MatchParticipantOverview;
import com.example.mmrtest.dto.MatchResultType;
import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.MetricCard;
import com.example.mmrtest.dto.TimelineBucket;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MatchAnalysisService {

    private final RiotMatchService riotMatchService;

    public MatchAnalysisService(RiotMatchService riotMatchService) {
        this.riotMatchService = riotMatchService;
    }

    public MatchAnalysisDetail buildMatchAnalysis(String puuid, String matchId) {
        MatchSummary summary = riotMatchService.fetchMatchDetail(puuid, matchId);
        if (summary == null) {
            return null;
        }

        Map<String, Object> rawMatch = riotMatchService.fetchMatchRaw(matchId);
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

        Map<String, Object> me = findParticipantByPuuid(participants, puuid);
        if (me == null) {
            return null;
        }

        MatchAnalysisDetail detail = new MatchAnalysisDetail();
        detail.setMatchId(matchId);
        detail.setPuuid(puuid);
        detail.setSummary(summary);
        detail.setResultType(summary.getResultType());

        List<MatchParticipantOverview> blueTeam = new ArrayList<>();
        List<MatchParticipantOverview> redTeam = new ArrayList<>();

        for (Map<String, Object> participant : participants) {
            MatchParticipantOverview overview = buildParticipantOverview(participant, puuid);
            if (overview.getTeamId() == 100) {
                blueTeam.add(overview);
            } else {
                redTeam.add(overview);
            }
        }

        detail.setBlueTeamPlayers(blueTeam);
        detail.setRedTeamPlayers(redTeam);

        Map<String, Object> laneOpponent = findLaneOpponent(me, participants);
        detail.setLaneComparison(buildLaneComparison(me, laneOpponent));

        if (summary.getResultType() == MatchResultType.REMAKE || summary.getResultType() == MatchResultType.INVALID) {
            detail.setMetricCards(new ArrayList<>());
            detail.setTimelineBuckets(new ArrayList<>());
            detail.setCoachingComments(buildExcludedMatchComments(summary));
            return detail;
        }

        detail.setMetricCards(buildMetricCards(me, participants, summary));
        detail.setCoachingComments(buildCoachingComments(me, summary, detail.getLaneComparison()));
        detail.setTimelineBuckets(buildTimelineBuckets(matchId, me, laneOpponent, summary));

        return detail;
    }

    private List<CoachingComment> buildExcludedMatchComments(MatchSummary summary) {
        List<CoachingComment> comments = new ArrayList<>();

        if (summary.getResultType() == MatchResultType.REMAKE) {
            comments.add(new CoachingComment(
                    "warning",
                    "다시하기 경기",
                    "이 경기는 다시하기로 종료되어 승패 및 자체 점수 계산에서 제외됩니다."
            ));
            comments.add(new CoachingComment(
                    "warning",
                    "상세 지표 제외",
                    "REMAKE 경기는 플레이 시간이 너무 짧아 성장 지표, 시간대별 추이, 성과 카드 계산을 생략합니다."
            ));
            return comments;
        }

        comments.add(new CoachingComment(
                "warning",
                "분석 제외 경기",
                "이 경기는 비정상 종료 또는 분석 제외 경기로 분류되어 상세 지표 계산을 생략합니다."
        ));
        comments.add(new CoachingComment(
                "warning",
                "상세 지표 제외",
                "INVALID 경기는 일반 승패 경기와 같은 기준으로 비교하기 어려워 상세 카드와 타임라인을 비워 둡니다."
        ));
        return comments;
    }

    private MatchParticipantOverview buildParticipantOverview(Map<String, Object> participant, String myPuuid) {
        MatchParticipantOverview overview = new MatchParticipantOverview();

        overview.setParticipantId(getInt(participant, "participantId", 0));
        overview.setRiotId(buildRiotId(participant));
        overview.setChampionName(getString(participant, "championName", "Unknown"));
        overview.setTeamId(getInt(participant, "teamId", 0));
        overview.setTeamPosition(resolvePosition(participant));
        overview.setMe(myPuuid.equals(getString(participant, "puuid", "")));

        overview.setKills(getInt(participant, "kills", 0));
        overview.setDeaths(getInt(participant, "deaths", 0));
        overview.setAssists(getInt(participant, "assists", 0));
        overview.setTotalCs(
                getInt(participant, "totalMinionsKilled", 0)
                        + getInt(participant, "neutralMinionsKilled", 0)
        );
        overview.setGoldEarned(getInt(participant, "goldEarned", 0));
        overview.setDamageToChampions(getInt(participant, "totalDamageDealtToChampions", 0));
        overview.setItems(Arrays.asList(
                getInt(participant, "item0", 0),
                getInt(participant, "item1", 0),
                getInt(participant, "item2", 0),
                getInt(participant, "item3", 0),
                getInt(participant, "item4", 0),
                getInt(participant, "item5", 0),
                getInt(participant, "item6", 0)
        ));

        return overview;
    }

    private LaneOpponentComparison buildLaneComparison(Map<String, Object> me, Map<String, Object> opponent) {
        if (me == null) {
            return null;
        }

        LaneOpponentComparison comparison = new LaneOpponentComparison();
        comparison.setMyChampionName(getString(me, "championName", "Unknown"));
        comparison.setMyPosition(resolvePosition(me));
        comparison.setMyCs(getTotalCs(me));
        comparison.setMyGoldEarned(getInt(me, "goldEarned", 0));
        comparison.setMyDamageToChampions(getInt(me, "totalDamageDealtToChampions", 0));

        if (opponent == null) {
            comparison.setOpponentChampionName("Unknown");
            comparison.setOpponentPosition("UNKNOWN");
            comparison.setOpponentCs(0);
            comparison.setOpponentGoldEarned(0);
            comparison.setOpponentDamageToChampions(0);
            comparison.setCsDiff(comparison.getMyCs());
            comparison.setGoldDiff(comparison.getMyGoldEarned());
            comparison.setDamageDiff(comparison.getMyDamageToChampions());
            return comparison;
        }

        comparison.setOpponentChampionName(getString(opponent, "championName", "Unknown"));
        comparison.setOpponentPosition(resolvePosition(opponent));
        comparison.setOpponentCs(getTotalCs(opponent));
        comparison.setOpponentGoldEarned(getInt(opponent, "goldEarned", 0));
        comparison.setOpponentDamageToChampions(getInt(opponent, "totalDamageDealtToChampions", 0));

        comparison.setCsDiff(comparison.getMyCs() - comparison.getOpponentCs());
        comparison.setGoldDiff(comparison.getMyGoldEarned() - comparison.getOpponentGoldEarned());
        comparison.setDamageDiff(comparison.getMyDamageToChampions() - comparison.getOpponentDamageToChampions());

        return comparison;
    }

    private List<MetricCard> buildMetricCards(
            Map<String, Object> me,
            List<Map<String, Object>> participants,
            MatchSummary summary
    ) {
        List<MetricCard> cards = new ArrayList<>();
        if (me == null || summary == null) {
            return cards;
        }

        int durationMinutes = Math.max(summary.getGameDurationMinutes(), 1);
        int myTeamId = getInt(me, "teamId", 0);

        int teamKills = participants.stream()
                .filter(p -> getInt(p, "teamId", 0) == myTeamId)
                .mapToInt(p -> getInt(p, "kills", 0))
                .sum();

        int teamDamage = participants.stream()
                .filter(p -> getInt(p, "teamId", 0) == myTeamId)
                .mapToInt(p -> getInt(p, "totalDamageDealtToChampions", 0))
                .sum();

        double kp = teamKills == 0
                ? 0.0
                : ((getInt(me, "kills", 0) + getInt(me, "assists", 0)) * 100.0 / teamKills);

        double csPerMin = getTotalCs(me) / (double) durationMinutes;
        double goldPerMin = getInt(me, "goldEarned", 0) / (double) durationMinutes;
        double damagePerMin = getInt(me, "totalDamageDealtToChampions", 0) / (double) durationMinutes;
        double damageShare = teamDamage == 0
                ? 0.0
                : (getInt(me, "totalDamageDealtToChampions", 0) * 100.0 / teamDamage);

        double visionScore = getInt(me, "visionScore", 0);
        double timeDead = getInt(me, "totalTimeSpentDead", 0);

        cards.add(new MetricCard(
                "kp",
                "킬 관여",
                round1(kp),
                "%",
                50.0,
                clampScore((int) Math.round(kp * 1.4)),
                "팀 킬 중 본인이 관여한 비율입니다."
        ));

        cards.add(new MetricCard(
                "cspm",
                "CS / 분",
                round2(csPerMin),
                "",
                6.0,
                clampScore((int) Math.round(csPerMin * 12)),
                "게임 시간 대비 파밍 유지력을 보여줍니다."
        ));

        cards.add(new MetricCard(
                "gpm",
                "골드 / 분",
                round1(goldPerMin),
                "",
                380.0,
                clampScore((int) Math.round(goldPerMin / 6.0)),
                "분당 골드 수급 속도입니다."
        ));

        cards.add(new MetricCard(
                "dpm",
                "딜량 / 분",
                round1(damagePerMin),
                "",
                500.0,
                clampScore((int) Math.round(damagePerMin / 10.0)),
                "분당 챔피언 대상 피해량입니다."
        ));

        cards.add(new MetricCard(
                "damageShare",
                "팀 내 딜 비중",
                round1(damageShare),
                "%",
                20.0,
                clampScore((int) Math.round(damageShare * 2.2)),
                "아군 총 챔피언 딜 중 본인이 차지한 비율입니다."
        ));

        cards.add(new MetricCard(
                "vision",
                "시야 점수",
                round1(visionScore),
                "",
                25.0,
                clampScore((int) Math.round(visionScore * 2.0)),
                "와드 설치/제거 등 시야 기여를 나타냅니다."
        ));

        cards.add(new MetricCard(
                "deadTime",
                "죽은 시간",
                round1(timeDead),
                "초",
                90.0,
                clampScore(100 - (int) Math.round(timeDead / 3.0)),
                "죽어 있던 시간이 길수록 운영 손실이 커집니다."
        ));

        return cards;
    }

    private List<CoachingComment> buildCoachingComments(
            Map<String, Object> me,
            MatchSummary summary,
            LaneOpponentComparison laneComparison
    ) {
        List<CoachingComment> comments = new ArrayList<>();
        if (me == null || summary == null) {
            return comments;
        }

        MatchResultType resultType = summary.getResultType();
        int kills = getInt(me, "kills", 0);
        int deaths = getInt(me, "deaths", 0);
        int assists = getInt(me, "assists", 0);
        double kda = deaths == 0 ? kills + assists : (kills + assists) / (double) deaths;

        if (resultType == MatchResultType.WIN) {
            comments.add(new CoachingComment(
                    "good",
                    "승리 반영",
                    "이번 경기는 승리로 집계되며 기본 점수 상승 대상입니다."
            ));
        } else if (resultType == MatchResultType.LOSS) {
            comments.add(new CoachingComment(
                    "bad",
                    "패배 반영",
                    "이번 경기는 패배로 집계되며 기본 점수 하락 대상입니다."
            ));
        }

        if (kda >= 4.0) {
            comments.add(new CoachingComment(
                    "good",
                    "교전 효율 우수",
                    String.format("KDA %.2f로 교전 효율이 높았습니다.", kda)
            ));
        } else if (kda <= 2.0) {
            comments.add(new CoachingComment(
                    "bad",
                    "데스 관리 필요",
                    String.format("KDA %.2f로 데스 관리 개선 여지가 있습니다.", kda)
            ));
        } else {
            comments.add(new CoachingComment(
                    "warning",
                    "교전 무난",
                    String.format("KDA %.2f로 평균적인 교전 흐름이었습니다.", kda)
            ));
        }

        if (laneComparison != null) {
            if (laneComparison.getGoldDiff() >= 1500) {
                comments.add(new CoachingComment(
                        "good",
                        "맞라인 우위",
                        "상대 라이너보다 골드 우위를 크게 벌렸습니다."
                ));
            } else if (laneComparison.getGoldDiff() <= -1500) {
                comments.add(new CoachingComment(
                        "bad",
                        "맞라인 열세",
                        "상대 라이너에게 골드 격차를 허용했습니다."
                ));
            } else {
                comments.add(new CoachingComment(
                        "warning",
                        "라인전 비등",
                        "맞라인 구도는 큰 차이 없이 비슷하게 흘렀습니다."
                ));
            }
        }

        comments.add(new CoachingComment(
                "warning",
                "타임라인 기반 분석",
                "타임라인 프레임이 있으면 실제 시간축 데이터로, 없으면 추정 버킷으로 성장 추이를 표시합니다."
        ));

        return comments;
    }

    private List<TimelineBucket> buildTimelineBuckets(
            String matchId,
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary
    ) {
        try {
            Map<String, Object> timelineRaw = riotMatchService.fetchMatchTimelineRaw(matchId);
            List<TimelineBucket> actual = buildActualTimelineBuckets(timelineRaw, me, opponent, summary);
            if (!actual.isEmpty()) {
                return actual;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buildEstimatedTimelineBuckets(me, opponent, summary);
    }

    private List<TimelineBucket> buildActualTimelineBuckets(
            Map<String, Object> timelineRaw,
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary
    ) {
        if (timelineRaw == null || me == null || summary == null) {
            return Collections.emptyList();
        }

        Map<String, Object> info = asMap(timelineRaw.get("info"));
        if (info == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> frames = asListOfMaps(info.get("frames"));
        if (frames.isEmpty()) {
            return Collections.emptyList();
        }

        int myParticipantId = getInt(me, "participantId", 0);
        int opponentParticipantId = opponent == null ? 0 : getInt(opponent, "participantId", 0);

        if (myParticipantId == 0) {
            return Collections.emptyList();
        }

        List<Integer> checkpoints = buildCheckpoints(summary.getGameDurationMinutes());
        List<TimelineBucket> buckets = new ArrayList<>();

        for (int minute : checkpoints) {
            long targetTimestamp = minute * 60_000L;
            Map<String, Object> frame = findFrameAtOrBefore(frames, targetTimestamp);
            if (frame == null) {
                continue;
            }

            Map<String, Object> myFrame = getParticipantFrame(frame, myParticipantId);
            if (myFrame == null) {
                continue;
            }

            Map<String, Object> oppFrame = opponentParticipantId == 0 ? null : getParticipantFrame(frame, opponentParticipantId);

            TimelineEventStats eventStats = collectEventStatsUntil(frames, targetTimestamp, myParticipantId);

            int myGold = getInt(myFrame, "totalGold", 0);
            int myCs = getInt(myFrame, "minionsKilled", 0) + getInt(myFrame, "jungleMinionsKilled", 0);
            int myLevel = getInt(myFrame, "level", 1);
            int myXp = getInt(myFrame, "xp", 0);

            int oppGold = oppFrame == null ? 0 : getInt(oppFrame, "totalGold", 0);
            int oppCs = oppFrame == null ? 0 : getInt(oppFrame, "minionsKilled", 0) + getInt(oppFrame, "jungleMinionsKilled", 0);
            int oppLevel = oppFrame == null ? 1 : getInt(oppFrame, "level", 1);
            int oppXp = oppFrame == null ? 0 : getInt(oppFrame, "xp", 0);

            int goldDiff = myGold - oppGold;
            int csDiff = myCs - oppCs;
            int xpDiff = myXp - oppXp;

            double growth = 50.0
                    + goldDiff / 150.0
                    + csDiff * 1.2
                    + xpDiff / 120.0;

            double combat = 45.0
                    + eventStats.kills * 9.0
                    + eventStats.assists * 5.0
                    - eventStats.deaths * 12.0;

            double map = 40.0
                    + eventStats.wardsPlaced * 2.5
                    + eventStats.wardsKilled * 4.0
                    + eventStats.objectiveScore * 6.0;

            double survival = 70.0 - eventStats.deaths * 12.0;

            double impact = growth * 0.35
                    + combat * 0.30
                    + map * 0.20
                    + survival * 0.15;

            TimelineBucket bucket = new TimelineBucket();
            bucket.setMinute(minute);
            bucket.setTotalGold(myGold);
            bucket.setTotalCs(myCs);
            bucket.setLevel(myLevel);
            bucket.setGoldDiffVsLane(goldDiff);
            bucket.setCsDiffVsLane(csDiff);
            bucket.setXpDiffVsLane(xpDiff);
            bucket.setGrowthScore(round1(clampDouble(growth, 0, 100)));
            bucket.setCombatScore(round1(clampDouble(combat, 0, 100)));
            bucket.setMapScore(round1(clampDouble(map, 0, 100)));
            bucket.setSurvivalScore(round1(clampDouble(survival, 0, 100)));
            bucket.setImpactScore(round1(clampDouble(impact, 0, 100)));

            buckets.add(bucket);
        }

        return buckets;
    }

    private TimelineEventStats collectEventStatsUntil(
            List<Map<String, Object>> frames,
            long targetTimestamp,
            int myParticipantId
    ) {
        TimelineEventStats stats = new TimelineEventStats();

        for (Map<String, Object> frame : frames) {
            List<Map<String, Object>> events = asListOfMaps(frame.get("events"));
            for (Map<String, Object> event : events) {
                long eventTimestamp = getLong(event, "timestamp", 0L);
                if (eventTimestamp > targetTimestamp) {
                    continue;
                }

                String type = getString(event, "type", "");

                if ("CHAMPION_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    int victimId = getInt(event, "victimId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));

                    if (killerId == myParticipantId) {
                        stats.kills++;
                    }
                    if (victimId == myParticipantId) {
                        stats.deaths++;
                    }
                    if (assistingIds.contains(myParticipantId)) {
                        stats.assists++;
                    }
                }

                if ("WARD_PLACED".equals(type)) {
                    int creatorId = getInt(event, "creatorId", 0);
                    if (creatorId == myParticipantId) {
                        stats.wardsPlaced++;
                    }
                }

                if ("WARD_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    if (killerId == myParticipantId) {
                        stats.wardsKilled++;
                    }
                }

                if ("ELITE_MONSTER_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));
                    int score = objectiveMonsterScore(getString(event, "monsterType", ""));

                    if (killerId == myParticipantId) {
                        stats.objectiveScore += score;
                    } else if (assistingIds.contains(myParticipantId)) {
                        stats.objectiveScore += Math.max(1, score / 2);
                    }
                }

                if ("BUILDING_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));

                    if (killerId == myParticipantId) {
                        stats.objectiveScore += 2;
                    } else if (assistingIds.contains(myParticipantId)) {
                        stats.objectiveScore += 1;
                    }
                }
            }
        }

        return stats;
    }

    private int objectiveMonsterScore(String monsterType) {
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

    private List<TimelineBucket> buildEstimatedTimelineBuckets(
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary
    ) {
        List<TimelineBucket> buckets = new ArrayList<>();
        if (me == null || summary == null) {
            return buckets;
        }

        int duration = Math.max(summary.getGameDurationMinutes(), 1);
        int finalGold = getInt(me, "goldEarned", 0);
        int finalCs = getTotalCs(me);
        int finalLevel = getInt(me, "champLevel", 1);

        int oppGold = opponent == null ? 0 : getInt(opponent, "goldEarned", 0);
        int oppCs = opponent == null ? 0 : getTotalCs(opponent);
        int oppLevel = opponent == null ? 1 : getInt(opponent, "champLevel", 1);

        List<Integer> checkpoints = buildCheckpoints(duration);

        for (int minute : checkpoints) {
            double ratio = minute / (double) duration;

            TimelineBucket bucket = new TimelineBucket();
            bucket.setMinute(minute);
            bucket.setTotalGold((int) Math.round(finalGold * ratio));
            bucket.setTotalCs((int) Math.round(finalCs * ratio));
            bucket.setLevel(Math.max(1, (int) Math.round(finalLevel * ratio)));

            bucket.setGoldDiffVsLane((int) Math.round((finalGold - oppGold) * ratio));
            bucket.setCsDiffVsLane((int) Math.round((finalCs - oppCs) * ratio));
            bucket.setXpDiffVsLane((int) Math.round((finalLevel - oppLevel) * ratio));

            double growth = 50.0
                    + bucket.getGoldDiffVsLane() / 120.0
                    + bucket.getCsDiffVsLane() * 0.8
                    + bucket.getXpDiffVsLane() * 8.0;

            double combat = 50.0
                    + (getInt(me, "kills", 0) + getInt(me, "assists", 0) - getInt(me, "deaths", 0)) * ratio * 5.0;

            double map = 45.0 + getInt(me, "visionScore", 0) * ratio * 0.6;
            double survival = 60.0 - getInt(me, "deaths", 0) * ratio * 6.0;
            double impact = growth * 0.35 + combat * 0.30 + map * 0.20 + survival * 0.15;

            bucket.setGrowthScore(round1(clampDouble(growth, 0, 100)));
            bucket.setCombatScore(round1(clampDouble(combat, 0, 100)));
            bucket.setMapScore(round1(clampDouble(map, 0, 100)));
            bucket.setSurvivalScore(round1(clampDouble(survival, 0, 100)));
            bucket.setImpactScore(round1(clampDouble(impact, 0, 100)));

            buckets.add(bucket);
        }

        return buckets;
    }

    private List<Integer> buildCheckpoints(int durationMinutes) {
        List<Integer> checkpoints = new ArrayList<>();
        checkpoints.add(0);

        for (int minute = 5; minute < durationMinutes; minute += 5) {
            checkpoints.add(minute);
        }

        if (checkpoints.get(checkpoints.size() - 1) != durationMinutes) {
            checkpoints.add(durationMinutes);
        }

        return checkpoints;
    }

    private Map<String, Object> findParticipantByPuuid(List<Map<String, Object>> participants, String puuid) {
        for (Map<String, Object> participant : participants) {
            if (puuid.equals(getString(participant, "puuid", ""))) {
                return participant;
            }
        }
        return null;
    }

    private Map<String, Object> findLaneOpponent(
            Map<String, Object> me,
            List<Map<String, Object>> participants
    ) {
        if (me == null) {
            return null;
        }

        int myTeamId = getInt(me, "teamId", 0);
        String myPosition = resolvePosition(me);

        Map<String, Object> fallback = null;

        for (Map<String, Object> participant : participants) {
            if (getInt(participant, "teamId", 0) == myTeamId) {
                continue;
            }

            if (fallback == null) {
                fallback = participant;
            }

            String opponentPosition = resolvePosition(participant);
            if (myPosition.equals(opponentPosition)) {
                return participant;
            }
        }

        return fallback;
    }

    private String resolvePosition(Map<String, Object> participant) {
        String individualPosition = getString(participant, "individualPosition", "");
        if (isUsablePosition(individualPosition)) {
            return individualPosition;
        }

        String teamPosition = getString(participant, "teamPosition", "");
        if (isUsablePosition(teamPosition)) {
            return teamPosition;
        }

        String lane = getString(participant, "lane", "");
        if (isUsablePosition(lane)) {
            return lane;
        }

        return "UNKNOWN";
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

    private int getTotalCs(Map<String, Object> participant) {
        return getInt(participant, "totalMinionsKilled", 0)
                + getInt(participant, "neutralMinionsKilled", 0);
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

    private int clampScore(int score) {
        return Math.max(0, Math.min(100, score));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
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

    @SuppressWarnings("unchecked")
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

    private static class TimelineEventStats {
        int kills;
        int assists;
        int deaths;
        int wardsPlaced;
        int wardsKilled;
        int objectiveScore;
    }
}