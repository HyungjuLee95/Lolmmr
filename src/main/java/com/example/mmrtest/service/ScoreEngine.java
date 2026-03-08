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

    private static final int WIN_BASE_DELTA = 18;
    private static final int LOSS_BASE_DELTA = -18;

    private static final Map<String, Double> ROLE_WEIGHT = Map.of(
            "TOP", 1.00,
            "JUNGLE", 1.10,
            "MIDDLE", 1.00,
            "BOTTOM", 1.05,
            "UTILITY", 0.95,
            "SUPPORT", 0.95);

    private static final Map<String, Double> ROLE_CSPM_BASELINE = Map.of(
            "TOP", 6.3,
            "JUNGLE", 5.8,
            "MIDDLE", 7.1,
            "BOTTOM", 7.7,
            "UTILITY", 1.6,
            "SUPPORT", 1.6);

    private static final Map<String, Double> ROLE_GPM_BASELINE = Map.of(
            "TOP", 390.0,
            "JUNGLE", 410.0,
            "MIDDLE", 420.0,
            "BOTTOM", 435.0,
            "UTILITY", 300.0,
            "SUPPORT", 300.0);

    @Value("${score.calibration-log-enabled:true}")
    private boolean calibrationLogEnabled;

    public ScoreResult calculateScore(List<MatchSummary> matches, int baseScore) {
        if (matches == null || matches.isEmpty()) {
            return buildScoreResult(
                    baseScore,
                    baseScore,
                    0,
                    0,
                    0,
                    0,
                    0.0,
                    0.0,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new LinkedHashMap<>());
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

        List<Integer> scoreHistory = new ArrayList<>();
        List<Integer> scoreDeltaHistory = new ArrayList<>();
        List<Integer> performanceHistory = new ArrayList<>();

        Map<String, MutableRoleAccumulator> roleAcc = new LinkedHashMap<>();

        for (MatchSummary match : sortedMatches) {
            if (match.isRemake()) {
                remakeCount++;
            }
            if (match.isInvalid()) {
                invalidCount++;
            }

            if (!match.isCountedGame()) {
                match.setPerformanceScore(0);
                scoreDeltaHistory.add(0);
                performanceHistory.add(0);
                scoreHistory.add(currentScore);
                continue;
            }

            countedGames++;

            String role = normalizeRole(match.getTeamPosition());
            int performanceScore = calculatePerformanceScore(match, role);
            match.setPerformanceScore(performanceScore);

            int baseDelta = match.isWin() ? WIN_BASE_DELTA : LOSS_BASE_DELTA;
            int performanceDelta = calculatePerformanceDelta(role, performanceScore);

            int finalDelta = baseDelta + performanceDelta;
            currentScore += finalDelta;

            deltaSum += finalDelta;
            performanceSum += performanceScore;

            scoreDeltaHistory.add(finalDelta);
            performanceHistory.add(performanceScore);
            scoreHistory.add(currentScore);

            MutableRoleAccumulator acc = roleAcc.computeIfAbsent(role, key -> new MutableRoleAccumulator());
            acc.games += 1;
            acc.deltaSum += finalDelta;
            acc.performanceSum += performanceScore;

            if (calibrationLogEnabled && log.isDebugEnabled()) {
                log.debug(
                        "[score-calibration] matchId={} role={} counted=true win={} perf={} baseDelta={} perfDelta={} finalDelta={} scoreAfter={}",
                        match.getMatchId(),
                        role,
                        match.isWin(),
                        performanceScore,
                        baseDelta,
                        performanceDelta,
                        finalDelta,
                        currentScore);
            }
        }

        double averageDelta = countedGames == 0 ? 0.0 : round2(deltaSum / (double) countedGames);
        double averagePerformance = countedGames == 0 ? 0.0 : round2(performanceSum / (double) countedGames);

        Map<String, ScoreResult.RoleStat> roleStats = new LinkedHashMap<>();
        for (Map.Entry<String, MutableRoleAccumulator> entry : roleAcc.entrySet()) {
            MutableRoleAccumulator acc = entry.getValue();
            double roleAvgPerf = round2(acc.performanceSum / (double) acc.games);
            double roleAvgDelta = round2(acc.deltaSum / (double) acc.games);
            roleStats.put(entry.getKey(), new ScoreResult.RoleStat(acc.games, roleAvgPerf, roleAvgDelta));
        }

        String grade = gradeFromScore(currentScore);

        if (calibrationLogEnabled) {
            log.info(
                    "[score-calibration] baseScore={} finalScore={} grade={} countedGames={} avgPerf={} avgDelta={} roleStats={}",
                    baseScore,
                    currentScore,
                    grade,
                    countedGames,
                    averagePerformance,
                    averageDelta,
                    roleStats);
        }

        return buildScoreResult(
                currentScore,
                baseScore,
                countedGames,
                sortedMatches.size(),
                remakeCount,
                invalidCount,
                averageDelta,
                averagePerformance,
                scoreHistory,
                scoreDeltaHistory,
                performanceHistory,
                roleStats);
    }

    private ScoreResult buildScoreResult(
            int currentScore,
            int baseScore,
            int countedGames,
            int sampleCount,
            int remakeCount,
            int invalidCount,
            double averageDelta,
            double averagePerformance,
            List<Integer> scoreHistory,
            List<Integer> scoreDeltaHistory,
            List<Integer> performanceHistory,
            Map<String, ScoreResult.RoleStat> roleStats) {
        ScoreResult result = new ScoreResult();
        result.setCurrentScore(currentScore);
        result.setTotalScore(currentScore);
        result.setBaseScore(baseScore);
        result.setCountedGames(countedGames);
        result.setSampleCount(sampleCount);
        result.setRemakeCount(remakeCount);
        result.setInvalidCount(invalidCount);
        result.setExcludedCount(remakeCount + invalidCount);
        result.setAverageDelta(averageDelta);
        result.setAveragePerformance(averagePerformance);
        result.setGrade(gradeFromScore(currentScore));
        result.setScoreHistory(scoreHistory);
        result.setScoreDeltaHistory(scoreDeltaHistory);
        result.setPerformanceHistory(performanceHistory);
        result.setRoleStats(roleStats);
        return result;
    }

    private int calculatePerformanceScore(MatchSummary match, String role) {
        int duration = Math.max(match.getGameDurationMinutes(), 1);

        double kda = match.getDeaths() == 0
                ? match.getKills() + match.getAssists()
                : (double) (match.getKills() + match.getAssists()) / Math.max(match.getDeaths(), 1);

        double csPerMin = match.getTotalCs() / (double) duration;
        double goldPerMin = match.getGoldEarned() / (double) duration;

        double csBase = ROLE_CSPM_BASELINE.getOrDefault(role, 6.0);
        double gpmBase = ROLE_GPM_BASELINE.getOrDefault(role, 380.0);

        double kdaScore = clamp((kda / 3.5) * 100.0, 0, 100);
        double csScore = clamp((csPerMin / csBase) * 100.0, 0, 100);
        double gpmScore = clamp((goldPerMin / gpmBase) * 100.0, 0, 100);

        double composite = kdaScore * 0.45 + csScore * 0.30 + gpmScore * 0.25;
        return (int) Math.round(clamp(composite, 0, 100));
    }

    private int calculatePerformanceDelta(String role, int performanceScore) {
        double weight = ROLE_WEIGHT.getOrDefault(role, 1.0);

        int centered = performanceScore - 50;
        int normalized = centered / 5;
        int weighted = (int) Math.round(normalized * weight);

        return Math.max(-12, Math.min(12, weighted));
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return "UNKNOWN";
        }

        String role = rawRole.toUpperCase(Locale.ROOT);

        if ("MID".equals(role))
            return "MIDDLE";
        if ("ADC".equals(role) || "BOT".equals(role))
            return "BOTTOM";
        if ("SUP".equals(role))
            return "SUPPORT";

        return role;
    }

    private String gradeFromScore(int score) {
        if (score >= 1280)
            return "S+";
        if (score >= 1200)
            return "S";
        if (score >= 1120)
            return "A";
        if (score >= 1040)
            return "B";
        if (score >= 960)
            return "C";
        if (score >= 880)
            return "D";
        return "F";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static class MutableRoleAccumulator {
        private int games;
        private double performanceSum;
        private double deltaSum;
    }
}
