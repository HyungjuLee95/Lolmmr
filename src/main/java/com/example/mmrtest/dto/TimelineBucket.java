package com.example.mmrtest.dto;

public class TimelineBucket {
    private int minute;

    private int totalGold;
    private int totalCs;
    private int level;

    private int goldDiffVsLane;
    private int csDiffVsLane;
    private int xpDiffVsLane;

    private double growthScore;
    private double combatScore;
    private double mapScore;
    private double survivalScore;
    private double impactScore;

    public TimelineBucket() {
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public int getTotalGold() {
        return totalGold;
    }

    public void setTotalGold(int totalGold) {
        this.totalGold = totalGold;
    }

    public int getTotalCs() {
        return totalCs;
    }

    public void setTotalCs(int totalCs) {
        this.totalCs = totalCs;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getGoldDiffVsLane() {
        return goldDiffVsLane;
    }

    public void setGoldDiffVsLane(int goldDiffVsLane) {
        this.goldDiffVsLane = goldDiffVsLane;
    }

    public int getCsDiffVsLane() {
        return csDiffVsLane;
    }

    public void setCsDiffVsLane(int csDiffVsLane) {
        this.csDiffVsLane = csDiffVsLane;
    }

    public int getXpDiffVsLane() {
        return xpDiffVsLane;
    }

    public void setXpDiffVsLane(int xpDiffVsLane) {
        this.xpDiffVsLane = xpDiffVsLane;
    }

    public double getGrowthScore() {
        return growthScore;
    }

    public void setGrowthScore(double growthScore) {
        this.growthScore = growthScore;
    }

    public double getCombatScore() {
        return combatScore;
    }

    public void setCombatScore(double combatScore) {
        this.combatScore = combatScore;
    }

    public double getMapScore() {
        return mapScore;
    }

    public void setMapScore(double mapScore) {
        this.mapScore = mapScore;
    }

    public double getSurvivalScore() {
        return survivalScore;
    }

    public void setSurvivalScore(double survivalScore) {
        this.survivalScore = survivalScore;
    }

    public double getImpactScore() {
        return impactScore;
    }

    public void setImpactScore(double impactScore) {
        this.impactScore = impactScore;
    }
}