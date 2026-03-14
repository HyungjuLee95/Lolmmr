package com.example.mmrtest.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;

@Component
public class ScoreEngine {

    private static final Logger log = LoggerFactory.getLogger(ScoreEngine.class);

    private static final int WIN_BASE_DELTA = 20;
    private static final int LOSS_BASE_DELTA = -20;
    private static final int LEAVER_EXTRA_PENALTY = 20;

    private static final Map<String, Double> ROLE_KP_BASELINE = Map.of(
            "TOP", 0.42,
            "JUNGLE", 0.58,
            "MIDDLE", 0.52,
            "BOTTOM", 0.57,
            "UTILITY", 0.63,
            "SUPPORT", 0.63,
            "UNKNOWN", 0.50
    );

    private static final Map<String, Double> ROLE_OBJECTIVE_BASELINE = Map.of(
            "TOP", 4.0,
            "JUNGLE", 8.0,
            "MIDDLE", 4.5,
            "BOTTOM", 3.5,
            "UTILITY", 5.5,
            "SUPPORT", 5.5,
            "UNKNOWN", 4.5
    );

    private static final Map<String, Double> ROLE_VISION_BASELINE = Map.of(
            "TOP", 1.00,
            "JUNGLE", 1.35,
            "MIDDLE", 0.95,
            "BOTTOM", 0.80,
            "UTILITY", 2.30,
            "SUPPORT", 2.30,
            "UNKNOWN", 1.10
    );

    private static final Map<String, Double> ROLE_DAMAGE_CONVERSION_BASELINE = Map.of(
            "TOP", 0.95,
            "JUNGLE", 0.95,
            "MIDDLE", 1.05,
            "BOTTOM", 1.10,
            "UTILITY", 0.82,
            "SUPPORT", 0.82,
            "UNKNOWN", 1.00
    );

    private static final Map<String, Double> ROLE_TIME_ALIVE_BASELINE = Map.of(
            "TOP", 0.84,
            "JUNGLE", 0.83,
            "MIDDLE", 0.82,
            "BOTTOM", 0.80,
            "UTILITY", 0.82,
            "SUPPORT", 0.82,
            "UNKNOWN", 0.82
    );

    private static final Map<String, Double> ROLE_THROW_PENALTY_CAP = Map.of(
            "TOP", 10.0,
            "JUNGLE", 10.0,
            "MIDDLE", 10.0,
            "BOTTOM", 9.0,
            "UTILITY", 8.0,
            "SUPPORT", 8.0,
            "UNKNOWN", 9.0
    );

    @Value("${score.calibration-log-enabled:true}")
    private boolean calibrationLogEnabled;

    public ScoreResult calculateScore(List<MatchSummary> matches, int baseScore) {
        if (matches == null || matches.isEmpty()) {
            return buildEmptyResult(baseScore);
        }

        List<MatchSummary> sortedMatches = matches.stream()
                .filter(match -> match != null)
                .sorted(Comparator.comparingLong(MatchSummary::getGameEndTimeStamp))
                .collect(Collectors.toList());

        int currentScore = baseScore;
        int countedGames = 0;
        int remakeCount = 0;
        int invalidCount = 0;

        int deltaSum = 0;
        int performanceSum = 0;
        int baseDeltaSum = 0;
        int performanceDeltaSum = 0;
        double perfIndexSum = 0.0;

        List<Integer> scoreHistory = new ArrayList<>();
        List<Integer> scoreDeltaHistory = new ArrayList<>();
        List<Integer> performanceHistory = new ArrayList<>();
        List<Double> perfIndexHistory = new ArrayList<>();
        List<Integer> baseDeltaHistory = new ArrayList<>();
        List<Integer> performanceDeltaHistory = new ArrayList<>();

        Map<String, MutableRoleAccumulator> roleAcc = new LinkedHashMap<>();

        for (MatchSummary match : sortedMatches) {
            if (match.isRemake()) {
                remakeCount++;
            }
            if (match.isInvalid()) {
                invalidCount++;
            }

            if (!match.isCountedGame()) {
                resetNonCountedMatch(match, currentScore);
                scoreHistory.add(currentScore);
                scoreDeltaHistory.add(0);
                performanceHistory.add(0);
                perfIndexHistory.add(0.0);
                baseDeltaHistory.add(0);
                performanceDeltaHistory.add(0);
                continue;
            }

            countedGames++;

            String role = normalizeRole(match.getTeamPosition());
            int baseDelta = match.isWin() ? WIN_BASE_DELTA : LOSS_BASE_DELTA;
            double alpha = tierAlpha(currentScore);

            double growthScore = calculateGrowthScore(match);
            double teamplayScore = calculateTeamplayScore(match, role);
            double survivalScore = calculateSurvivalScore(match, role);
            double efficiencyScore = calculateEfficiencyScore(match, role, survivalScore);

            double perfRaw = (0.30 * growthScore) + (0.45 * teamplayScore) + (0.25 * efficiencyScore);
            double perfIndex = Math.tanh(perfRaw);

            int performanceScore = clampInt((int) Math.round((perfIndex + 1.0) * 50.0), 0, 100);
            int performanceDelta = (int) Math.round(alpha * 10.0 * perfIndex);
            int finalDelta = baseDelta + performanceDelta;

            if (match.isWin()) {
                finalDelta = clampInt(finalDelta, 10, 30);
            } else {
                finalDelta = clampInt(finalDelta, -30, -10);
            }

            if (match.isLeaver()) {
                finalDelta -= LEAVER_EXTRA_PENALTY;
            }

            currentScore += finalDelta;

            match.setPerformanceScore(performanceScore);
            match.setBaseDelta(baseDelta);
            match.setPerformanceDelta(performanceDelta);
            match.setFinalDelta(finalDelta);
            match.setPerfIndex(round4(perfIndex));
            match.setGrowthScore(round4(growthScore));
            match.setTeamplayScore(round4(teamplayScore));
            match.setEfficiencyScore(round4(efficiencyScore));
            match.setSurvivalScore(round4(survivalScore));
            match.setScoreTier(tierFromScore(currentScore));

            deltaSum += finalDelta;
            performanceSum += performanceScore;
            baseDeltaSum += baseDelta;
            performanceDeltaSum += performanceDelta;
            perfIndexSum += perfIndex;

            scoreHistory.add(currentScore);
            scoreDeltaHistory.add(finalDelta);
            performanceHistory.add(performanceScore);
            perfIndexHistory.add(round4(perfIndex));
            baseDeltaHistory.add(baseDelta);
            performanceDeltaHistory.add(performanceDelta);

            MutableRoleAccumulator acc = roleAcc.computeIfAbsent(role, key -> new MutableRoleAccumulator());
            acc.games += 1;
            acc.deltaSum += finalDelta;
            acc.performanceSum += performanceScore;
            acc.baseDeltaSum += baseDelta;
            acc.performanceDeltaSum += performanceDelta;
            acc.perfIndexSum += perfIndex;

            if (calibrationLogEnabled && log.isDebugEnabled()) {
                log.debug(
                        "[score-calibration-v1] matchId={} role={} counted=true win={} growth={} teamplay={} efficiency={} perfIndex={} perf={} baseDelta={} perfDelta={} finalDelta={} scoreAfter={}",
                        match.getMatchId(),
                        role,
                        match.isWin(),
                        round4(growthScore),
                        round4(teamplayScore),
                        round4(efficiencyScore),
                        round4(perfIndex),
                        performanceScore,
                        baseDelta,
                        performanceDelta,
                        finalDelta,
                        currentScore
                );
            }
        }

        double averageDelta = countedGames == 0 ? 0.0 : round2(deltaSum / (double) countedGames);
        double averagePerformance = countedGames == 0 ? 0.0 : round2(performanceSum / (double) countedGames);
        double averagePerfIndex = countedGames == 0 ? 0.0 : round4(perfIndexSum / countedGames);
        double averageBaseDelta = countedGames == 0 ? 0.0 : round2(baseDeltaSum / (double) countedGames);
        double averagePerformanceDelta = countedGames == 0 ? 0.0 : round2(performanceDeltaSum / (double) countedGames);

        Map<String, ScoreResult.RoleStat> roleStats = new LinkedHashMap<>();
        for (Map.Entry<String, MutableRoleAccumulator> entry : roleAcc.entrySet()) {
            MutableRoleAccumulator acc = entry.getValue();
            roleStats.put(entry.getKey(), new ScoreResult.RoleStat(
                    acc.games,
                    round2(acc.performanceSum / (double) acc.games),
                    round2(acc.deltaSum / (double) acc.games),
                    round4(acc.perfIndexSum / acc.games),
                    round2(acc.baseDeltaSum / (double) acc.games),
                    round2(acc.performanceDeltaSum / (double) acc.games)
            ));
        }

        ScoreResult result = new ScoreResult();
        result.setCurrentScore(currentScore);
        result.setTotalScore(currentScore);
        result.setBaseScore(baseScore);
        result.setGrade(gradeFromAveragePerformance(averagePerformance));
        result.setScoreTier(tierFromScore(currentScore));
        result.setCountedGames(countedGames);
        result.setSampleCount(sortedMatches.size());
        result.setRemakeCount(remakeCount);
        result.setInvalidCount(invalidCount);
        result.setExcludedCount(remakeCount + invalidCount);
        result.setAverageDelta(averageDelta);
        result.setAveragePerformance(averagePerformance);
        result.setAveragePerfIndex(averagePerfIndex);
        result.setAverageBaseDelta(averageBaseDelta);
        result.setAveragePerformanceDelta(averagePerformanceDelta);
        result.setScoreHistory(scoreHistory);
        result.setScoreDeltaHistory(scoreDeltaHistory);
        result.setPerformanceHistory(performanceHistory);
        result.setPerfIndexHistory(perfIndexHistory);
        result.setBaseDeltaHistory(baseDeltaHistory);
        result.setPerformanceDeltaHistory(performanceDeltaHistory);
        result.setRoleStats(roleStats);

        if (calibrationLogEnabled) {
            log.info(
                    "[score-calibration-v1] baseScore={} finalScore={} tier={} grade={} countedGames={} avgPerf={} avgPerfIndex={} avgDelta={} roleStats={}",
                    baseScore,
                    currentScore,
                    result.getScoreTier(),
                    result.getGrade(),
                    countedGames,
                    averagePerformance,
                    averagePerfIndex,
                    averageDelta,
                    roleStats
            );
        }

        return result;
    }

    private ScoreResult buildEmptyResult(int baseScore) {
        ScoreResult result = new ScoreResult();
        result.setCurrentScore(baseScore);
        result.setTotalScore(baseScore);
        result.setBaseScore(baseScore);
        result.setGrade("C");
        result.setScoreTier(tierFromScore(baseScore));
        result.setCountedGames(0);
        result.setSampleCount(0);
        result.setExcludedCount(0);
        result.setRemakeCount(0);
        result.setInvalidCount(0);
        result.setAverageDelta(0.0);
        result.setAveragePerformance(0.0);
        result.setAveragePerfIndex(0.0);
        result.setAverageBaseDelta(0.0);
        result.setAveragePerformanceDelta(0.0);
        result.setScoreHistory(new ArrayList<>());
        result.setScoreDeltaHistory(new ArrayList<>());
        result.setPerformanceHistory(new ArrayList<>());
        result.setPerfIndexHistory(new ArrayList<>());
        result.setBaseDeltaHistory(new ArrayList<>());
        result.setPerformanceDeltaHistory(new ArrayList<>());
        result.setRoleStats(new LinkedHashMap<>());
        return result;
    }

    private void resetNonCountedMatch(MatchSummary match, int currentScore) {
        match.setPerformanceScore(0);
        match.setBaseDelta(0);
        match.setPerformanceDelta(0);
        match.setFinalDelta(0);
        match.setPerfIndex(0.0);
        match.setGrowthScore(0.0);
        match.setTeamplayScore(0.0);
        match.setEfficiencyScore(0.0);
        match.setSurvivalScore(0.0);
        match.setScoreTier(tierFromScore(currentScore));
    }

    private double calculateGrowthScore(MatchSummary match) {
        double goldScore = normalizeDiff(match.getGoldDiff15(), 500.0);
        double csScore = normalizeDiff(match.getCsDiff15(), 20.0);
        double xpScore = normalizeDiff(match.getXpDiff15(), 600.0);

        return clampDouble((0.50 * goldScore) + (0.25 * csScore) + (0.25 * xpScore), -1.0, 1.0);
    }

    private double calculateTeamplayScore(MatchSummary match, String role) {
        double kpScore = normalizeRatio(match.getKillParticipation(), ROLE_KP_BASELINE.getOrDefault(role, 0.50));
        double objectiveScore = normalizeRatio(match.getObjectiveParticipationScore(), ROLE_OBJECTIVE_BASELINE.getOrDefault(role, 4.5));
        double visionScore = normalizeRatio(match.getVisionPerMinute(), ROLE_VISION_BASELINE.getOrDefault(role, 1.10));

        double kpWeight = switch (role) {
            case "TOP" -> 0.30;
            case "JUNGLE" -> 0.32;
            case "MIDDLE" -> 0.34;
            case "BOTTOM" -> 0.38;
            case "UTILITY", "SUPPORT" -> 0.34;
            default -> 0.33;
        };

        double objectiveWeight = switch (role) {
            case "JUNGLE" -> 0.43;
            case "UTILITY", "SUPPORT" -> 0.28;
            default -> 0.33;
        };

        double visionWeight = 1.0 - kpWeight - objectiveWeight;

        return clampDouble(
                (kpWeight * kpScore) + (objectiveWeight * objectiveScore) + (visionWeight * visionScore),
                -1.0,
                1.0
        );
    }

    private double calculateSurvivalScore(MatchSummary match, String role) {
        double aliveRatioScore = normalizeRatio(match.getTimeAliveRatio(), ROLE_TIME_ALIVE_BASELINE.getOrDefault(role, 0.82));
        double deathPenaltyScore = normalizePenalty(match.getThrowDeathPenalty(), ROLE_THROW_PENALTY_CAP.getOrDefault(role, 9.0));

        return clampDouble((0.65 * aliveRatioScore) + (0.35 * deathPenaltyScore), -1.0, 1.0);
    }

    private double calculateEfficiencyScore(MatchSummary match, String role, double survivalScore) {
        double damageScore = normalizeRatio(match.getDamageConversion(), ROLE_DAMAGE_CONVERSION_BASELINE.getOrDefault(role, 1.0));
        return clampDouble((0.60 * damageScore) + (0.40 * survivalScore), -1.0, 1.0);
    }

    private double normalizeDiff(double value, double scale) {
        if (scale <= 0.0) {
            return 0.0;
        }
        return clampDouble(value / scale, -1.0, 1.0);
    }

    private double normalizeRatio(double value, double baseline) {
        if (baseline <= 0.0 || value <= 0.0) {
            return value <= 0.0 ? -1.0 : 0.0;
        }
        return clampDouble((value / baseline) - 1.0, -1.0, 1.0);
    }

    private double normalizePenalty(double penalty, double maxPenalty) {
        if (maxPenalty <= 0.0) {
            return 0.0;
        }
        return clampDouble(1.0 - (penalty / maxPenalty), -1.0, 1.0);
    }

    private double tierAlpha(int score) {
        if (score >= 2200) return 0.60;
        if (score >= 1900) return 0.70;
        if (score >= 1600) return 0.80;
        if (score >= 1300) return 0.90;
        return 1.00;
    }

    private String tierFromScore(int score) {
        if (score >= 2200) return "DIAMOND";
        if (score >= 1900) return "EMERALD";
        if (score >= 1600) return "PLATINUM";
        if (score >= 1300) return "GOLD";
        if (score >= 1000) return "SILVER";
        return "IRON/BRONZE";
    }

    private String gradeFromAveragePerformance(double averagePerformance) {
        if (averagePerformance >= 85.0) return "S+";
        if (averagePerformance >= 75.0) return "S";
        if (averagePerformance >= 65.0) return "A";
        if (averagePerformance >= 55.0) return "B";
        if (averagePerformance >= 45.0) return "C";
        return "D";
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return "UNKNOWN";
        }

        String role = rawRole.toUpperCase(Locale.ROOT);

        if ("MID".equals(role)) return "MIDDLE";
        if ("ADC".equals(role) || "BOT".equals(role)) return "BOTTOM";
        if ("SUP".equals(role)) return "SUPPORT";

        return role;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private static class MutableRoleAccumulator {
        private int games;
        private int performanceSum;
        private int deltaSum;
        private int baseDeltaSum;
        private int performanceDeltaSum;
        private double perfIndexSum;
    }
}