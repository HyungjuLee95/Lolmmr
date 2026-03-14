package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScoreResult {

    // ===== 기존 핵심 결과 =====
    private int currentScore;
    private double totalScore;
    private String grade;
    private int baseScore;

    // ===== v1 확장: 내부 점수 티어 =====
    private String scoreTier;

    // ===== 경기 집계 =====
    private int countedGames;
    private int sampleCount;
    private int excludedCount;
    private int remakeCount;
    private int invalidCount;

    // ===== 평균 지표 =====
    private double averageDelta;
    private double averagePerformance;

    // ===== v1 확장 평균값 =====
    private double averagePerfIndex;
    private double averageBaseDelta;
    private double averagePerformanceDelta;

    // ===== 히스토리 =====
    private List<Integer> scoreHistory = new ArrayList<>();
    private List<Integer> scoreDeltaHistory = new ArrayList<>();
    private List<Integer> performanceHistory = new ArrayList<>();

    // ===== v1 확장 히스토리 =====
    private List<Double> perfIndexHistory = new ArrayList<>();
    private List<Integer> baseDeltaHistory = new ArrayList<>();
    private List<Integer> performanceDeltaHistory = new ArrayList<>();

    // ===== 포지션 통계 =====
    private Map<String, RoleStat> roleStats = new LinkedHashMap<>();

    public ScoreResult() {
    }

    public ScoreResult(int currentScore, List<Integer> scoreHistory) {
        this.currentScore = currentScore;
        this.totalScore = currentScore;
        this.scoreHistory = scoreHistory == null ? new ArrayList<>() : scoreHistory;
        this.grade = "C";
        this.scoreTier = "SILVER";
    }

    /**
     * 기존 코드 호환용 생성자
     */
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
        this.scoreHistory = scoreHistory == null ? new ArrayList<>() : scoreHistory;
        this.scoreDeltaHistory = scoreDeltaHistory == null ? new ArrayList<>() : scoreDeltaHistory;
        this.performanceHistory = performanceHistory == null ? new ArrayList<>() : performanceHistory;
        this.roleStats = roleStats == null ? new LinkedHashMap<>() : roleStats;
        this.scoreTier = "";
        this.perfIndexHistory = new ArrayList<>();
        this.baseDeltaHistory = new ArrayList<>();
        this.performanceDeltaHistory = new ArrayList<>();
    }

    /**
     * v1 확장 생성자
     */
    public ScoreResult(
            int currentScore,
            double totalScore,
            String grade,
            String scoreTier,
            int baseScore,
            int countedGames,
            int sampleCount,
            int excludedCount,
            int remakeCount,
            int invalidCount,
            double averageDelta,
            double averagePerformance,
            double averagePerfIndex,
            double averageBaseDelta,
            double averagePerformanceDelta,
            List<Integer> scoreHistory,
            List<Integer> scoreDeltaHistory,
            List<Integer> performanceHistory,
            List<Double> perfIndexHistory,
            List<Integer> baseDeltaHistory,
            List<Integer> performanceDeltaHistory,
            Map<String, RoleStat> roleStats
    ) {
        this.currentScore = currentScore;
        this.totalScore = totalScore;
        this.grade = grade;
        this.scoreTier = scoreTier;
        this.baseScore = baseScore;
        this.countedGames = countedGames;
        this.sampleCount = sampleCount;
        this.excludedCount = excludedCount;
        this.remakeCount = remakeCount;
        this.invalidCount = invalidCount;
        this.averageDelta = averageDelta;
        this.averagePerformance = averagePerformance;
        this.averagePerfIndex = averagePerfIndex;
        this.averageBaseDelta = averageBaseDelta;
        this.averagePerformanceDelta = averagePerformanceDelta;
        this.scoreHistory = scoreHistory == null ? new ArrayList<>() : scoreHistory;
        this.scoreDeltaHistory = scoreDeltaHistory == null ? new ArrayList<>() : scoreDeltaHistory;
        this.performanceHistory = performanceHistory == null ? new ArrayList<>() : performanceHistory;
        this.perfIndexHistory = perfIndexHistory == null ? new ArrayList<>() : perfIndexHistory;
        this.baseDeltaHistory = baseDeltaHistory == null ? new ArrayList<>() : baseDeltaHistory;
        this.performanceDeltaHistory = performanceDeltaHistory == null ? new ArrayList<>() : performanceDeltaHistory;
        this.roleStats = roleStats == null ? new LinkedHashMap<>() : roleStats;
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

    public String getScoreTier() {
        return scoreTier;
    }

    public void setScoreTier(String scoreTier) {
        this.scoreTier = scoreTier;
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

    public double getAveragePerfIndex() {
        return averagePerfIndex;
    }

    public void setAveragePerfIndex(double averagePerfIndex) {
        this.averagePerfIndex = averagePerfIndex;
    }

    public double getAverageBaseDelta() {
        return averageBaseDelta;
    }

    public void setAverageBaseDelta(double averageBaseDelta) {
        this.averageBaseDelta = averageBaseDelta;
    }

    public double getAveragePerformanceDelta() {
        return averagePerformanceDelta;
    }

    public void setAveragePerformanceDelta(double averagePerformanceDelta) {
        this.averagePerformanceDelta = averagePerformanceDelta;
    }

    public List<Integer> getScoreHistory() {
        return scoreHistory;
    }

    public void setScoreHistory(List<Integer> scoreHistory) {
        this.scoreHistory = scoreHistory == null ? new ArrayList<>() : scoreHistory;
    }

    public List<Integer> getScoreDeltaHistory() {
        return scoreDeltaHistory;
    }

    public void setScoreDeltaHistory(List<Integer> scoreDeltaHistory) {
        this.scoreDeltaHistory = scoreDeltaHistory == null ? new ArrayList<>() : scoreDeltaHistory;
    }

    public List<Integer> getPerformanceHistory() {
        return performanceHistory;
    }

    public void setPerformanceHistory(List<Integer> performanceHistory) {
        this.performanceHistory = performanceHistory == null ? new ArrayList<>() : performanceHistory;
    }

    public List<Double> getPerfIndexHistory() {
        return perfIndexHistory;
    }

    public void setPerfIndexHistory(List<Double> perfIndexHistory) {
        this.perfIndexHistory = perfIndexHistory == null ? new ArrayList<>() : perfIndexHistory;
    }

    public List<Integer> getBaseDeltaHistory() {
        return baseDeltaHistory;
    }

    public void setBaseDeltaHistory(List<Integer> baseDeltaHistory) {
        this.baseDeltaHistory = baseDeltaHistory == null ? new ArrayList<>() : baseDeltaHistory;
    }

    public List<Integer> getPerformanceDeltaHistory() {
        return performanceDeltaHistory;
    }

    public void setPerformanceDeltaHistory(List<Integer> performanceDeltaHistory) {
        this.performanceDeltaHistory = performanceDeltaHistory == null ? new ArrayList<>() : performanceDeltaHistory;
    }

    public Map<String, RoleStat> getRoleStats() {
        return roleStats;
    }

    public void setRoleStats(Map<String, RoleStat> roleStats) {
        this.roleStats = roleStats == null ? new LinkedHashMap<>() : roleStats;
    }

    public static class RoleStat {
        private int games;
        private double averagePerformance;
        private double averageDelta;

        // v1 확장
        private double averagePerfIndex;
        private double averageBaseDelta;
        private double averagePerformanceDelta;

        public RoleStat() {
        }

        public RoleStat(int games, double averagePerformance, double averageDelta) {
            this.games = games;
            this.averagePerformance = averagePerformance;
            this.averageDelta = averageDelta;
        }

        public RoleStat(
                int games,
                double averagePerformance,
                double averageDelta,
                double averagePerfIndex,
                double averageBaseDelta,
                double averagePerformanceDelta
        ) {
            this.games = games;
            this.averagePerformance = averagePerformance;
            this.averageDelta = averageDelta;
            this.averagePerfIndex = averagePerfIndex;
            this.averageBaseDelta = averageBaseDelta;
            this.averagePerformanceDelta = averagePerformanceDelta;
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

        public double getAveragePerfIndex() {
            return averagePerfIndex;
        }

        public void setAveragePerfIndex(double averagePerfIndex) {
            this.averagePerfIndex = averagePerfIndex;
        }

        public double getAverageBaseDelta() {
            return averageBaseDelta;
        }

        public void setAverageBaseDelta(double averageBaseDelta) {
            this.averageBaseDelta = averageBaseDelta;
        }

        public double getAveragePerformanceDelta() {
            return averagePerformanceDelta;
        }

        public void setAveragePerformanceDelta(double averagePerformanceDelta) {
            this.averagePerformanceDelta = averagePerformanceDelta;
        }

        @Override
        public String toString() {
            return "RoleStat{" +
                    "games=" + games +
                    ", averagePerformance=" + averagePerformance +
                    ", averageDelta=" + averageDelta +
                    ", averagePerfIndex=" + averagePerfIndex +
                    ", averageBaseDelta=" + averageBaseDelta +
                    ", averagePerformanceDelta=" + averagePerformanceDelta +
                    '}';
        }
    }
}