package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.List;

public class ScoreResult {
    private int currentScore;
    private double totalScore;
    private String grade;
    private int baseScore;
    private int countedGames;
    private List<Integer> scoreHistory = new ArrayList<>();
    private List<Integer> scoreDeltaHistory = new ArrayList<>();
    private List<Integer> performanceHistory = new ArrayList<>();

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
            List<Integer> scoreHistory,
            List<Integer> scoreDeltaHistory,
            List<Integer> performanceHistory
    ) {
        this.currentScore = currentScore;
        this.totalScore = totalScore;
        this.grade = grade;
        this.baseScore = baseScore;
        this.countedGames = countedGames;
        this.scoreHistory = scoreHistory;
        this.scoreDeltaHistory = scoreDeltaHistory;
        this.performanceHistory = performanceHistory;
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
}
