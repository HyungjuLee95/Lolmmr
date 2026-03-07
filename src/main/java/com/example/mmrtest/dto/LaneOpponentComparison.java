package com.example.mmrtest.dto;

public class LaneOpponentComparison {
    private String myChampionName;
    private String opponentChampionName;
    private String myPosition;
    private String opponentPosition;

    private int myCs;
    private int opponentCs;
    private int myGoldEarned;
    private int opponentGoldEarned;
    private int myDamageToChampions;
    private int opponentDamageToChampions;

    private int csDiff;
    private int goldDiff;
    private int damageDiff;

    public LaneOpponentComparison() {
    }

    public String getMyChampionName() {
        return myChampionName;
    }

    public void setMyChampionName(String myChampionName) {
        this.myChampionName = myChampionName;
    }

    public String getOpponentChampionName() {
        return opponentChampionName;
    }

    public void setOpponentChampionName(String opponentChampionName) {
        this.opponentChampionName = opponentChampionName;
    }

    public String getMyPosition() {
        return myPosition;
    }

    public void setMyPosition(String myPosition) {
        this.myPosition = myPosition;
    }

    public String getOpponentPosition() {
        return opponentPosition;
    }

    public void setOpponentPosition(String opponentPosition) {
        this.opponentPosition = opponentPosition;
    }

    public int getMyCs() {
        return myCs;
    }

    public void setMyCs(int myCs) {
        this.myCs = myCs;
    }

    public int getOpponentCs() {
        return opponentCs;
    }

    public void setOpponentCs(int opponentCs) {
        this.opponentCs = opponentCs;
    }

    public int getMyGoldEarned() {
        return myGoldEarned;
    }

    public void setMyGoldEarned(int myGoldEarned) {
        this.myGoldEarned = myGoldEarned;
    }

    public int getOpponentGoldEarned() {
        return opponentGoldEarned;
    }

    public void setOpponentGoldEarned(int opponentGoldEarned) {
        this.opponentGoldEarned = opponentGoldEarned;
    }

    public int getMyDamageToChampions() {
        return myDamageToChampions;
    }

    public void setMyDamageToChampions(int myDamageToChampions) {
        this.myDamageToChampions = myDamageToChampions;
    }

    public int getOpponentDamageToChampions() {
        return opponentDamageToChampions;
    }

    public void setOpponentDamageToChampions(int opponentDamageToChampions) {
        this.opponentDamageToChampions = opponentDamageToChampions;
    }

    public int getCsDiff() {
        return csDiff;
    }

    public void setCsDiff(int csDiff) {
        this.csDiff = csDiff;
    }

    public int getGoldDiff() {
        return goldDiff;
    }

    public void setGoldDiff(int goldDiff) {
        this.goldDiff = goldDiff;
    }

    public int getDamageDiff() {
        return damageDiff;
    }

    public void setDamageDiff(int damageDiff) {
        this.damageDiff = damageDiff;
    }
}