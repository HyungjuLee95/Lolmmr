package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchSummary {

    private String matchId;
    private MatchResultType resultType;

    private int kills;
    private int deaths;
    private int assists;
    private String championName;

    private List<Integer> items = new ArrayList<>();
    private List<String> teamMembers = new ArrayList<>();
    private List<String> teamChamps = new ArrayList<>();

    private int gameDurationMinutes;
    private int spell1Id;
    private int spell2Id;
    private int mainRuneId;
    private int subRuneId;
    private int totalCs;
    private int goldEarned;
    private int queueId;
    private long gameEndTimeStamp;
    private int performanceScore;

    private boolean invalid;
    private boolean countedGame;
    private boolean remake;
    private boolean win;
    private String displayResult;
    private boolean loss;

    public MatchSummary() {
    }

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public MatchResultType getResultType() {
        return resultType;
    }

    public void setResultType(MatchResultType resultType) {
        this.resultType = resultType;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public String getChampionName() {
        return championName;
    }

    public void setChampionName(String championName) {
        this.championName = championName;
    }

    public List<Integer> getItems() {
        return items;
    }

    public void setItems(List<Integer> items) {
        this.items = items;
    }

    public List<String> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<String> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public List<String> getTeamChamps() {
        return teamChamps;
    }

    public void setTeamChamps(List<String> teamChamps) {
        this.teamChamps = teamChamps;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public int getSpell1Id() {
        return spell1Id;
    }

    public void setSpell1Id(int spell1Id) {
        this.spell1Id = spell1Id;
    }

    public int getSpell2Id() {
        return spell2Id;
    }

    public void setSpell2Id(int spell2Id) {
        this.spell2Id = spell2Id;
    }

    public int getMainRuneId() {
        return mainRuneId;
    }

    public void setMainRuneId(int mainRuneId) {
        this.mainRuneId = mainRuneId;
    }

    public int getSubRuneId() {
        return subRuneId;
    }

    public void setSubRuneId(int subRuneId) {
        this.subRuneId = subRuneId;
    }

    public int getTotalCs() {
        return totalCs;
    }

    public void setTotalCs(int totalCs) {
        this.totalCs = totalCs;
    }

    public int getGoldEarned() {
        return goldEarned;
    }

    public void setGoldEarned(int goldEarned) {
        this.goldEarned = goldEarned;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getGameEndTimeStamp() {
        return gameEndTimeStamp;
    }

    public void setGameEndTimeStamp(long gameEndTimeStamp) {
        this.gameEndTimeStamp = gameEndTimeStamp;
    }

    public int getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(int performanceScore) {
        this.performanceScore = performanceScore;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public boolean isCountedGame() {
        return countedGame;
    }

    public void setCountedGame(boolean countedGame) {
        this.countedGame = countedGame;
    }

    public boolean isRemake() {
        return remake;
    }

    public void setRemake(boolean remake) {
        this.remake = remake;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public String getDisplayResult() {
        return displayResult;
    }

    public void setDisplayResult(String displayResult) {
        this.displayResult = displayResult;
    }

    public boolean isLoss() {
        return loss;
    }

    public void setLoss(boolean loss) {
        this.loss = loss;
    }
}