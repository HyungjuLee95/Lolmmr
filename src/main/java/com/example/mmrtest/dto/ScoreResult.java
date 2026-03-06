package com.example.mmrtest.dto;

import java.util.List;

public class ScoreResult {
    private int currentScore;
    private List<Integer> scoreHistory;

    public ScoreResult() {
    }

    public ScoreResult(int currentScore, List<Integer> scoreHistory) {
        this.currentScore = currentScore;
        this.scoreHistory = scoreHistory;
    }

    public int getCurrentScore() {
        return currentScore;
    }

    public void setCurrentScore(int currentScore) {
        this.currentScore = currentScore;
    }

    public List<Integer> getScoreHistory() {
        return scoreHistory;
    }

    public void setScoreHistory(List<Integer> scoreHistory) {
        this.scoreHistory = scoreHistory;
    }
}
