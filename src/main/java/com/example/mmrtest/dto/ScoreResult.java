package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreResult {
    private int currentScore;
    private double totalScore;
    private String grade;
    private int baseScore;
    private int countedGames;
    private int sampleCount;
    private int excludedCount;
    private int remakeCount;
    private int invalidCount;
    private double averageDelta;
    private double averagePerformance;
    private List<Integer> scoreHistory = new ArrayList<>();
    private List<Integer> scoreDeltaHistory = new ArrayList<>();
    private List<Integer> performanceHistory = new ArrayList<>();
    private Map<String, RoleStat> roleStats = new LinkedHashMap<>();

    public ScoreResult() {
    }

    public ScoreResult(int currentScore, List<Integer> scoreHistory) {
        this.currentScore = currentScore;
        this.totalScore = currentScore;
        this.scoreHistory = scoreHistory;
        this.grade = "C";
    }

    public ScoreResult(
            int currentScore,
            double totalScore,
            String grade,
            int baseScore,
            int countedGames,
            int sampleCount,
            int excludedCount,
            int remakeCount,
            int invalidCount,
            double averageDelta,
            double averagePerformance,
            List<Integer> scoreHistory,
            List<Integer> scoreDeltaHistory,
            List<Integer> performanceHistory,
            Map<String, RoleStat> roleStats
    ) {
        this.currentScore = currentScore;
        this.totalScore = totalScore;
        this.grade = grade;
        this.baseScore = baseScore;
        this.countedGames = countedGames;
        this.sampleCount = sampleCount;
        this.excludedCount = excludedCount;
        this.remakeCount = remakeCount;
        this.invalidCount = invalidCount;
        this.averageDelta = averageDelta;
        this.averagePerformance = averagePerformance;
        this.scoreHistory = scoreHistory;
        this.scoreDeltaHistory = scoreDeltaHistory;
        this.performanceHistory = performanceHistory;
        this.roleStats = roleStats;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(int currentScore) {
        this.currentScore = currentScore;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public int getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(int baseScore) {
        this.baseScore = baseScore;
    }

    public int getCountedGames() {
        return countedGames;
    }

    public void setCountedGames(int countedGames) {
        this.countedGames = countedGames;
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }

    public int getExcludedCount() {
        return excludedCount;
    }

    public void setExcludedCount(int excludedCount) {
        this.excludedCount = excludedCount;
    }

    public int getRemakeCount() {
        return remakeCount;
    }

    public void setRemakeCount(int remakeCount) {
        this.remakeCount = remakeCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public void setInvalidCount(int invalidCount) {
        this.invalidCount = invalidCount;
    }

    public double getAverageDelta() {
        return averageDelta;
    }

    public void setAverageDelta(double averageDelta) {
        this.averageDelta = averageDelta;
    }

    public double getAveragePerformance() {
        return averagePerformance;
    }

    public void setAveragePerformance(double averagePerformance) {
        this.averagePerformance = averagePerformance;
    }

    public List<Integer> getScoreHistory() {
        return scoreHistory;
    }

    public void setScoreHistory(List<Integer> scoreHistory) {
        this.scoreHistory = scoreHistory;
    }

    public List<Integer> getScoreDeltaHistory() {
        return scoreDeltaHistory;
    }

    public void setScoreDeltaHistory(List<Integer> scoreDeltaHistory) {
        this.scoreDeltaHistory = scoreDeltaHistory;
    }

    public List<Integer> getPerformanceHistory() {
        return performanceHistory;
    }

    public void setPerformanceHistory(List<Integer> performanceHistory) {
        this.performanceHistory = performanceHistory;
    }

    public Map<String, RoleStat> getRoleStats() {
        return roleStats;
    }

    public void setRoleStats(Map<String, RoleStat> roleStats) {
        this.roleStats = roleStats;
    }

    public static class RoleStat {
        private int games;
        private double averagePerformance;
        private double averageDelta;

        public RoleStat() {
        }

        public RoleStat(int games, double averagePerformance, double averageDelta) {
            this.games = games;
            this.averagePerformance = averagePerformance;
            this.averageDelta = averageDelta;
        }

        public int getGames() {
            return games;
        }

        public void setGames(int games) {
            this.games = games;
        }

        public double getAveragePerformance() {
            return averagePerformance;
        }

        public void setAveragePerformance(double averagePerformance) {
            this.averagePerformance = averagePerformance;
        }

        public double getAverageDelta() {
            return averageDelta;
        }

        public void setAverageDelta(double averageDelta) {
            this.averageDelta = averageDelta;
        }

        @Override
        public String toString() {
            return "RoleStat{" +
                    "games=" + games +
                    ", averagePerformance=" + averagePerformance +
                    ", averageDelta=" + averageDelta +
                    '}';
        }
    }
}
