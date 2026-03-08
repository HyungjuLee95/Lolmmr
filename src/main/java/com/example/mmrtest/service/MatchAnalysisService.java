package com.example.mmrtest.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.mmrtest.dto.CoachingComment;
import com.example.mmrtest.dto.LaneOpponentComparison;
import com.example.mmrtest.dto.MatchAnalysisDetail;
import com.example.mmrtest.dto.MatchParticipantOverview;
import com.example.mmrtest.dto.MatchResultType;
import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.MatchTeamOverview;
import com.example.mmrtest.dto.MetricCard;
import com.example.mmrtest.dto.TeamObjectiveSummary;
import com.example.mmrtest.dto.TimelineBucket;

@Service
public class MatchAnalysisService {

    private final RiotMatchService riotMatchService;

    public MatchAnalysisService(RiotMatchService riotMatchService) {
        this.riotMatchService = riotMatchService;
    }

    public MatchAnalysisDetail buildMatchAnalysis(String puuid, String matchId) {
        return buildMatchAnalysis(puuid, matchId, 3);
    }

    public MatchAnalysisDetail buildMatchAnalysis(String puuid, String matchId, int bucketMinutes) {
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

        Map<String, Object> timelineRaw = null;
        try {
            timelineRaw = riotMatchService.fetchMatchTimelineRaw(matchId);
        } catch (Exception ignored) {
            // 타임라인이 없어도 일부 분석은 가능
        }

        int normalizedBucketMinutes = normalizeBucketMinutes(bucketMinutes);

        MatchAnalysisDetail detail = new MatchAnalysisDetail();
        detail.setMatchId(matchId);
        detail.setPuuid(puuid);
        detail.setSummary(summary);
        detail.setResultType(summary.getResultType());
        detail.setBucketMinutes(normalizedBucketMinutes);

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

        detail.setBlueTeamSummary(buildTeamSummary(100, blueTeam, timelineRaw));
        detail.setRedTeamSummary(buildTeamSummary(200, redTeam, timelineRaw));

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
        detail.setTimelineBuckets(buildTimelineBuckets(timelineRaw, me, laneOpponent, summary, normalizedBucketMinutes));

        return detail;
    }

    private MatchTeamOverview buildTeamSummary(
            int teamId,
            List<MatchParticipantOverview> teamPlayers,
            Map<String, Object> timelineRaw
    ) {
        MatchTeamOverview summary = new MatchTeamOverview();
        summary.setTeamId(teamId);

        if (teamPlayers == null || teamPlayers.isEmpty()) {
            summary.setWin(false);
            summary.setKills(0);
            summary.setDeaths(0);
            summary.setTotalGold(0);
            summary.setObjectives(new TeamObjectiveSummary());
            return summary;
        }

        int kills = 0;
        int deaths = 0;
        int totalGold = 0;

        for (MatchParticipantOverview player : teamPlayers) {
            kills += player.getKills();
            deaths += player.getDeaths();
            totalGold += player.getGoldEarned();
        }

        summary.setWin(false);
        summary.setKills(kills);
        summary.setDeaths(deaths);
        summary.setTotalGold(totalGold);
        summary.setObjectives(buildObjectiveSummary(timelineRaw, teamId));

        return summary;
    }

    private TeamObjectiveSummary buildObjectiveSummary(Map<String, Object> timelineRaw, int teamId) {
        TeamObjectiveSummary objectives = new TeamObjectiveSummary();

        if (timelineRaw == null) {
            return objectives;
        }

        Map<String, Object> info = asMap(timelineRaw.get("info"));
        if (info == null) {
            return objectives;
        }

        List<Map<String, Object>> frames = asListOfMaps(info.get("frames"));
        if (frames.isEmpty()) {
            return objectives;
        }

        for (Map<String, Object> frame : frames) {
            List<Map<String, Object>> events = asListOfMaps(frame.get("events"));

            for (Map<String, Object> event : events) {
                String type = getString(event, "type", "");

                if ("ELITE_MONSTER_KILL".equals(type)) {
                    int killerTeamId = resolveEventTeamId(event);
                    if (killerTeamId != teamId) {
                        continue;
                    }

                    String monsterType = getString(event, "monsterType", "");
                    switch (monsterType) {
                        case "DRAGON" -> objectives.setDragons(objectives.getDragons() + 1);
                        case "RIFTHERALD" -> objectives.setHeralds(objectives.getHeralds() + 1);
                        case "BARON_NASHOR" -> objectives.setBarons(objectives.getBarons() + 1);
                        case "HORDE" -> objectives.setVoidgrubs(objectives.getVoidgrubs() + 1);
                        default -> {
                        }
                    }
                }

                if ("BUILDING_KILL".equals(type)) {
                    int killerTeamId = resolveEventTeamId(event);
                    if (killerTeamId != teamId) {
                        continue;
                    }

                    String buildingType = getString(event, "buildingType", "");
                    if ("TOWER_BUILDING".equals(buildingType)) {
                        objectives.setTowers(objectives.getTowers() + 1);
                    }
                }
            }
        }

        return objectives;
    }

    private int resolveEventTeamId(Map<String, Object> event) {
        int teamId = getInt(event, "teamId", 0);
        if (teamId == 100 || teamId == 200) {
            return teamId;
        }

        int killerId = getInt(event, "killerId", 0);
        if (killerId >= 1 && killerId <= 5) {
            return 100;
        }
        if (killerId >= 6 && killerId <= 10) {
            return 200;
        }

        return 0;
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
                "시간 구간별로 내 수치와 상대 수치를 함께 비교할 수 있도록 버킷 데이터를 구성했습니다."
        ));

        return comments;
    }

    private List<TimelineBucket> buildTimelineBuckets(
            Map<String, Object> timelineRaw,
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary,
            int bucketMinutes
    ) {
        try {
            List<TimelineBucket> actual = buildActualTimelineBuckets(timelineRaw, me, opponent, summary, bucketMinutes);
            if (!actual.isEmpty()) {
                return actual;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return buildEstimatedTimelineBuckets(me, opponent, summary, bucketMinutes);
    }

    private List<TimelineBucket> buildActualTimelineBuckets(
            Map<String, Object> timelineRaw,
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary,
            int bucketMinutes
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

        List<Integer> checkpoints = buildCheckpoints(summary.getGameDurationMinutes(), bucketMinutes);
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

            TimelineEventStats myEventStats = collectEventStatsUntil(frames, targetTimestamp, myParticipantId);
            TimelineEventStats oppEventStats = opponentParticipantId == 0
                    ? new TimelineEventStats()
                    : collectEventStatsUntil(frames, targetTimestamp, opponentParticipantId);

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

            double myGrowth = calculateGrowthScore(myGold, myCs, myXp, goldDiff, csDiff, xpDiff);
            double opponentGrowth = calculateGrowthScore(oppGold, oppCs, oppXp, -goldDiff, -csDiff, -xpDiff);

            double myCombat = calculateCombatScore(myEventStats);
            double opponentCombat = calculateCombatScore(oppEventStats);

            double myMap = calculateMapScore(myEventStats);
            double opponentMap = calculateMapScore(oppEventStats);

            double mySurvival = calculateSurvivalScore(myEventStats, myLevel);
            double opponentSurvival = calculateSurvivalScore(oppEventStats, oppLevel);

            double myImpact = calculateImpactScore(myGrowth, myCombat, myMap, mySurvival);
            double opponentImpact = calculateImpactScore(opponentGrowth, opponentCombat, opponentMap, opponentSurvival);

            TimelineBucket bucket = new TimelineBucket();
            bucket.setMinute(minute);

            bucket.setTotalGold(myGold);
            bucket.setTotalCs(myCs);
            bucket.setLevel(myLevel);

            bucket.setMyGold(myGold);
            bucket.setOpponentGold(oppGold);
            bucket.setMyCs(myCs);
            bucket.setOpponentCs(oppCs);
            bucket.setMyXp(myXp);
            bucket.setOpponentXp(oppXp);
            bucket.setMyLevel(myLevel);
            bucket.setOpponentLevel(oppLevel);

            bucket.setGoldDiffVsLane(goldDiff);
            bucket.setCsDiffVsLane(csDiff);
            bucket.setXpDiffVsLane(xpDiff);

            bucket.setGrowthScore(round1(myGrowth));
            bucket.setCombatScore(round1(myCombat));
            bucket.setMapScore(round1(myMap));
            bucket.setSurvivalScore(round1(mySurvival));
            bucket.setImpactScore(round1(myImpact));

            bucket.setMyGrowthScore(round1(myGrowth));
            bucket.setOpponentGrowthScore(round1(opponentGrowth));

            bucket.setMyCombatScore(round1(myCombat));
            bucket.setOpponentCombatScore(round1(opponentCombat));

            bucket.setMyMapScore(round1(myMap));
            bucket.setOpponentMapScore(round1(opponentMap));

            bucket.setMySurvivalScore(round1(mySurvival));
            bucket.setOpponentSurvivalScore(round1(opponentSurvival));

            bucket.setMyImpactScore(round1(myImpact));
            bucket.setOpponentImpactScore(round1(opponentImpact));

            buckets.add(bucket);
        }

        return buckets;
    }

    private List<TimelineBucket> buildEstimatedTimelineBuckets(
            Map<String, Object> me,
            Map<String, Object> opponent,
            MatchSummary summary,
            int bucketMinutes
    ) {
        List<TimelineBucket> buckets = new ArrayList<>();
        if (me == null || summary == null) {
            return buckets;
        }

        int duration = Math.max(summary.getGameDurationMinutes(), 1);

        int myFinalGold = getInt(me, "goldEarned", 0);
        int myFinalCs = getTotalCs(me);
        int myFinalLevel = getInt(me, "champLevel", 1);
        int myFinalXp = estimateXpFromLevel(myFinalLevel);

        int oppFinalGold = opponent == null ? 0 : getInt(opponent, "goldEarned", 0);
        int oppFinalCs = opponent == null ? 0 : getTotalCs(opponent);
        int oppFinalLevel = opponent == null ? 1 : getInt(opponent, "champLevel", 1);
        int oppFinalXp = estimateXpFromLevel(oppFinalLevel);

        int myFinalKills = getInt(me, "kills", 0);
        int myFinalAssists = getInt(me, "assists", 0);
        int myFinalDeaths = getInt(me, "deaths", 0);
        int myVision = getInt(me, "visionScore", 0);

        int oppFinalKills = opponent == null ? 0 : getInt(opponent, "kills", 0);
        int oppFinalAssists = opponent == null ? 0 : getInt(opponent, "assists", 0);
        int oppFinalDeaths = opponent == null ? 0 : getInt(opponent, "deaths", 0);
        int oppVision = opponent == null ? 0 : getInt(opponent, "visionScore", 0);

        List<Integer> checkpoints = buildCheckpoints(duration, bucketMinutes);

        for (int minute : checkpoints) {
            double ratio = minute / (double) duration;

            int myGold = (int) Math.round(myFinalGold * ratio);
            int myCs = (int) Math.round(myFinalCs * ratio);
            int myLevel = Math.max(1, (int) Math.round(myFinalLevel * ratio));
            int myXp = (int) Math.round(myFinalXp * ratio);

            int oppGold = (int) Math.round(oppFinalGold * ratio);
            int oppCs = (int) Math.round(oppFinalCs * ratio);
            int oppLevel = Math.max(1, (int) Math.round(oppFinalLevel * ratio));
            int oppXp = (int) Math.round(oppFinalXp * ratio);

            int goldDiff = myGold - oppGold;
            int csDiff = myCs - oppCs;
            int xpDiff = myXp - oppXp;

            TimelineEventStats myEventStats = new TimelineEventStats();
            myEventStats.kills = (int) Math.round(myFinalKills * ratio);
            myEventStats.assists = (int) Math.round(myFinalAssists * ratio);
            myEventStats.deaths = (int) Math.round(myFinalDeaths * ratio);
            myEventStats.wardsPlaced = (int) Math.round(myVision * ratio * 0.55);
            myEventStats.wardsKilled = (int) Math.round(myVision * ratio * 0.20);
            myEventStats.objectiveScore = (int) Math.round((myFinalKills + myFinalAssists) * ratio * 0.35);

            TimelineEventStats oppEventStats = new TimelineEventStats();
            oppEventStats.kills = (int) Math.round(oppFinalKills * ratio);
            oppEventStats.assists = (int) Math.round(oppFinalAssists * ratio);
            oppEventStats.deaths = (int) Math.round(oppFinalDeaths * ratio);
            oppEventStats.wardsPlaced = (int) Math.round(oppVision * ratio * 0.55);
            oppEventStats.wardsKilled = (int) Math.round(oppVision * ratio * 0.20);
            oppEventStats.objectiveScore = (int) Math.round((oppFinalKills + oppFinalAssists) * ratio * 0.35);

            double myGrowth = calculateGrowthScore(myGold, myCs, myXp, goldDiff, csDiff, xpDiff);
            double opponentGrowth = calculateGrowthScore(oppGold, oppCs, oppXp, -goldDiff, -csDiff, -xpDiff);

            double myCombat = calculateCombatScore(myEventStats);
            double opponentCombat = calculateCombatScore(oppEventStats);

            double myMap = calculateMapScore(myEventStats);
            double opponentMap = calculateMapScore(oppEventStats);

            double mySurvival = calculateSurvivalScore(myEventStats, myLevel);
            double opponentSurvival = calculateSurvivalScore(oppEventStats, oppLevel);

            double myImpact = calculateImpactScore(myGrowth, myCombat, myMap, mySurvival);
            double opponentImpact = calculateImpactScore(opponentGrowth, opponentCombat, opponentMap, opponentSurvival);

            TimelineBucket bucket = new TimelineBucket();
            bucket.setMinute(minute);

            bucket.setTotalGold(myGold);
            bucket.setTotalCs(myCs);
            bucket.setLevel(myLevel);

            bucket.setMyGold(myGold);
            bucket.setOpponentGold(oppGold);
            bucket.setMyCs(myCs);
            bucket.setOpponentCs(oppCs);
            bucket.setMyXp(myXp);
            bucket.setOpponentXp(oppXp);
            bucket.setMyLevel(myLevel);
            bucket.setOpponentLevel(oppLevel);

            bucket.setGoldDiffVsLane(goldDiff);
            bucket.setCsDiffVsLane(csDiff);
            bucket.setXpDiffVsLane(xpDiff);

            bucket.setGrowthScore(round1(myGrowth));
            bucket.setCombatScore(round1(myCombat));
            bucket.setMapScore(round1(myMap));
            bucket.setSurvivalScore(round1(mySurvival));
            bucket.setImpactScore(round1(myImpact));

            bucket.setMyGrowthScore(round1(myGrowth));
            bucket.setOpponentGrowthScore(round1(opponentGrowth));

            bucket.setMyCombatScore(round1(myCombat));
            bucket.setOpponentCombatScore(round1(opponentCombat));

            bucket.setMyMapScore(round1(myMap));
            bucket.setOpponentMapScore(round1(opponentMap));

            bucket.setMySurvivalScore(round1(mySurvival));
            bucket.setOpponentSurvivalScore(round1(opponentSurvival));

            bucket.setMyImpactScore(round1(myImpact));
            bucket.setOpponentImpactScore(round1(opponentImpact));

            buckets.add(bucket);
        }

        return buckets;
    }

    private double calculateGrowthScore(int gold, int cs, int xp, int goldDiff, int csDiff, int xpDiff) {
        double goldComponent = Math.min(35.0, gold / 450.0);
        double csComponent = Math.min(25.0, cs / 6.0);
        double xpComponent = Math.min(20.0, xp / 280.0);
        double diffComponent = clampDouble(
                goldDiff / 180.0 + csDiff * 0.9 + xpDiff / 160.0,
                -15.0,
                15.0
        );

        return clampDouble(10.0 + goldComponent + csComponent + xpComponent + diffComponent, 0.0, 100.0);
    }

    private double calculateCombatScore(TimelineEventStats stats) {
        double score = 40.0
                + stats.kills * 10.0
                + stats.assists * 5.0
                - stats.deaths * 12.0;
        return clampDouble(score, 0.0, 100.0);
    }

    private double calculateMapScore(TimelineEventStats stats) {
        double score = 35.0
                + stats.wardsPlaced * 2.0
                + stats.wardsKilled * 3.5
                + stats.objectiveScore * 5.0;
        return clampDouble(score, 0.0, 100.0);
    }

    private double calculateSurvivalScore(TimelineEventStats stats, int level) {
        double score = 80.0
                - stats.deaths * 12.0
                + Math.min(8.0, level * 0.6);
        return clampDouble(score, 0.0, 100.0);
    }

    private double calculateImpactScore(double growth, double combat, double map, double survival) {
        return clampDouble(
                growth * 0.35 + combat * 0.30 + map * 0.20 + survival * 0.15,
                0.0,
                100.0
        );
    }

    private int estimateXpFromLevel(int level) {
        int safeLevel = Math.max(1, level);
        return safeLevel * safeLevel * 100;
    }

    private TimelineEventStats collectEventStatsUntil(
            List<Map<String, Object>> frames,
            long targetTimestamp,
            int participantId
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

                    if (killerId == participantId) {
                        stats.kills++;
                    }
                    if (victimId == participantId) {
                        stats.deaths++;
                    }
                    if (assistingIds.contains(participantId)) {
                        stats.assists++;
                    }
                }

                if ("WARD_PLACED".equals(type)) {
                    int creatorId = getInt(event, "creatorId", 0);
                    if (creatorId == participantId) {
                        stats.wardsPlaced++;
                    }
                }

                if ("WARD_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    if (killerId == participantId) {
                        stats.wardsKilled++;
                    }
                }

                if ("ELITE_MONSTER_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));
                    int score = objectiveMonsterScore(getString(event, "monsterType", ""));

                    if (killerId == participantId) {
                        stats.objectiveScore += score;
                    } else if (assistingIds.contains(participantId)) {
                        stats.objectiveScore += Math.max(1, score / 2);
                    }
                }

                if ("BUILDING_KILL".equals(type)) {
                    int killerId = getInt(event, "killerId", 0);
                    List<Integer> assistingIds = getIntList(event.get("assistingParticipantIds"));

                    if (killerId == participantId) {
                        stats.objectiveScore += 2;
                    } else if (assistingIds.contains(participantId)) {
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

    private List<Integer> buildCheckpoints(int durationMinutes, int bucketMinutes) {
        int step = normalizeBucketMinutes(bucketMinutes);

        List<Integer> checkpoints = new ArrayList<>();
        checkpoints.add(0);

        for (int minute = step; minute < durationMinutes; minute += step) {
            checkpoints.add(minute);
        }

        if (checkpoints.get(checkpoints.size() - 1) != durationMinutes) {
            checkpoints.add(durationMinutes);
        }

        return checkpoints;
    }

    private int normalizeBucketMinutes(int bucketMinutes) {
        if (bucketMinutes == 5 || bucketMinutes == 10) {
            return bucketMinutes;
        }
        return 3;
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