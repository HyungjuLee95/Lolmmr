package com.example.mmrtest.dto;

public class MatchTeamOverview {
    private int teamId;
    private boolean win;
    private int kills;
    private int deaths;
    private int totalGold;
    private TeamObjectiveSummary objectives;

    public MatchTeamOverview() {
        this.objectives = new TeamObjectiveSummary();
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
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

    public int getTotalGold() {
        return totalGold;
    }

    public void setTotalGold(int totalGold) {
        this.totalGold = totalGold;
    }

    public TeamObjectiveSummary getObjectives() {
        return objectives;
    }

    public void setObjectives(TeamObjectiveSummary objectives) {
        this.objectives = objectives;
    }
}