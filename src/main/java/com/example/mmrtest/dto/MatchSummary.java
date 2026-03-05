package com.example.mmrtest.dto;

import java.util.List;

public class MatchSummary {
    private boolean win;
    private int kills;
    private int deaths;
    private int assists;
    private String championName;
    private List<Integer> items;
    private List<String> teamMembers;
    private List<String> teamChamps;
    private int gameDurationMinutes;
    private int spell1Id;
    private int spell2Id;
    private int mainRuneId;
    private int subRuneId;
    private int totalCs;
    private int goldEarned;
    private long gameEndTimeStamp;
    private int performanceScore;

    public MatchSummary() {
    }

    public MatchSummary(boolean win, int kills, int deaths, int assists, String championName, List<Integer> items,
                        List<String> teamMembers, List<String> teamChamps, int gameDurationMinutes,
                        int spell1Id, int spell2Id, int mainRuneId, int subRuneId,
                        int totalCs, int goldEarned, long gameEndTimeStamp, int performanceScore) {
        this.win = win;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.championName = championName;
        this.items = items;
        this.teamMembers = teamMembers;
        this.teamChamps = teamChamps;
        this.gameDurationMinutes = gameDurationMinutes;
        this.spell1Id = spell1Id;
        this.spell2Id = spell2Id;
        this.mainRuneId = mainRuneId;
        this.subRuneId = subRuneId;
        this.totalCs = totalCs;
        this.goldEarned = goldEarned;
        this.gameEndTimeStamp = gameEndTimeStamp;
        this.performanceScore = performanceScore;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
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
}
