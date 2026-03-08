package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ScoreEngine {

    private static final int WIN_BASE_DELTA = 18;
    private static final int LOSS_BASE_DELTA = -18;

    private static final Map<String, Double> ROLE_WEIGHT = Map.of(
            "TOP", 1.00,
            "JUNGLE", 1.10,
            "MIDDLE", 1.00,
            "BOTTOM", 1.05,
            "UTILITY", 0.95,
            "SUPPORT", 0.95
    );

    private static final Map<String, Double> ROLE_CSPM_BASELINE = Map.of(
            "TOP", 6.3,
            "JUNGLE", 5.8,
            "MIDDLE", 7.1,
            "BOTTOM", 7.7,
            "UTILITY", 1.6,
            "SUPPORT", 1.6
    );

    private static final Map<String, Double> ROLE_GPM_BASELINE = Map.of(
            "TOP", 390.0,
            "JUNGLE", 410.0,
            "MIDDLE", 420.0,
            "BOTTOM", 435.0,
            "UTILITY", 300.0,
            "SUPPORT", 300.0
    );

    public ScoreResult calculateScore(List<MatchSummary> matches, int baseScore) {
        if (matches == null || matches.isEmpty()) {
            return new ScoreResult(
                    baseScore,
                    baseScore,
                    gradeFromScore(baseScore),
                    baseScore,
                    0,
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        List<MatchSummary> sortedMatches = matches.stream()
                .filter(match -> match != null)
                .sorted(Comparator.comparingLong(MatchSummary::getGameEndTimeStamp))
                .collect(Collectors.toList());

        int currentScore = baseScore;
        int countedGames = 0;

        List<Integer> scoreHistory = new ArrayList<>();
        List<Integer> scoreDeltaHistory = new ArrayList<>();
        List<Integer> performanceHistory = new ArrayList<>();

        for (MatchSummary match : sortedMatches) {
            if (!match.isCountedGame()) {
                match.setPerformanceScore(0);
                scoreDeltaHistory.add(0);
                performanceHistory.add(0);
                scoreHistory.add(currentScore);
                continue;
            }

            countedGames++;

            int performanceScore = calculatePerformanceScore(match);
            match.setPerformanceScore(performanceScore);

            int baseDelta = match.isWin() ? WIN_BASE_DELTA : LOSS_BASE_DELTA;
            int performanceDelta = calculatePerformanceDelta(match.getTeamPosition(), performanceScore);

            int finalDelta = baseDelta + performanceDelta;
            currentScore += finalDelta;

            scoreDeltaHistory.add(finalDelta);
            performanceHistory.add(performanceScore);
            scoreHistory.add(currentScore);
        }

        return new ScoreResult(
                currentScore,
                currentScore,
                gradeFromScore(currentScore),
                baseScore,
                countedGames,
                scoreHistory,
                scoreDeltaHistory,
                performanceHistory
        );
    }

    private int calculatePerformanceScore(MatchSummary match) {
        int duration = Math.max(match.getGameDurationMinutes(), 1);

        double kda = match.getDeaths() == 0
                ? match.getKills() + match.getAssists()
                : (double) (match.getKills() + match.getAssists()) / Math.max(match.getDeaths(), 1);

        String role = normalizeRole(match.getTeamPosition());
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

    private int calculatePerformanceDelta(String teamPosition, int performanceScore) {
        String role = normalizeRole(teamPosition);
        double weight = ROLE_WEIGHT.getOrDefault(role, 1.0);

        int centered = performanceScore - 50;
        int normalized = centered / 5; // about -10 to +10
        int weighted = (int) Math.round(normalized * weight);

        return Math.max(-12, Math.min(12, weighted));
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

    private String gradeFromScore(int score) {
        if (score >= 1280) return "S+";
        if (score >= 1200) return "S";
        if (score >= 1120) return "A";
        if (score >= 1040) return "B";
        if (score >= 960) return "C";
        if (score >= 880) return "D";
        return "F";
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
