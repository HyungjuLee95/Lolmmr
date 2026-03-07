package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchParticipantOverview {
    private int participantId;
    private String riotId;
    private String championName;
    private int teamId;
    private String teamPosition;
    private boolean me;

    private int kills;
    private int deaths;
    private int assists;
    private int totalCs;
    private int goldEarned;
    private int damageToChampions;

    private List<Integer> items = new ArrayList<>();

    public MatchParticipantOverview() {
    }

    public MatchParticipantOverview(
            int participantId,
            String riotId,
            String championName,
            int teamId,
            String teamPosition,
            boolean me,
            int kills,
            int deaths,
            int assists,
            int totalCs,
            int goldEarned,
            int damageToChampions,
            List<Integer> items
    ) {
        this.participantId = participantId;
        this.riotId = riotId;
        this.championName = championName;
        this.teamId = teamId;
        this.teamPosition = teamPosition;
        this.me = me;
        this.kills = kills;
        this.deaths = deaths;
        this.assists = assists;
        this.totalCs = totalCs;
        this.goldEarned = goldEarned;
        this.damageToChampions = damageToChampions;
        this.items = items == null ? new ArrayList<>() : items;
    }

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public String getRiotId() {
        return riotId;
    }

    public void setRiotId(String riotId) {
        this.riotId = riotId;
    }

    public String getChampionName() {
        return championName;
    }

    public void setChampionName(String championName) {
        this.championName = championName;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public String getTeamPosition() {
        return teamPosition;
    }

    public void setTeamPosition(String teamPosition) {
        this.teamPosition = teamPosition;
    }

    public boolean isMe() {
        return me;
    }

    public void setMe(boolean me) {
        this.me = me;
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

    public int getDamageToChampions() {
        return damageToChampions;
    }

    public void setDamageToChampions(int damageToChampions) {
        this.damageToChampions = damageToChampions;
    }

    public List<Integer> getItems() {
        return items;
    }

    public void setItems(List<Integer> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }
}