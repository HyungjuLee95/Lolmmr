package com.example.mmrtest.entity.core;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "match_participant")
@IdClass(MatchParticipantId.class)
public class MatchParticipant {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Id
    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Column(name = "participant_id")
    private Integer participantId;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "team_position")
    private String teamPosition;

    @Column(name = "champion_name")
    private String championName;

    @Column(name = "kills")
    private Integer kills;

    @Column(name = "deaths")
    private Integer deaths;

    @Column(name = "assists")
    private Integer assists;

    @Column(name = "total_cs")
    private Integer totalCs;

    @Column(name = "gold_earned")
    private Integer goldEarned;

    @Column(name = "damage_to_champions")
    private Integer damageToChampions;

    @Column(name = "vision_score")
    private Integer visionScore;

    @Column(name = "wards_placed")
    private Integer wardsPlaced;

    @Column(name = "wards_killed")
    private Integer wardsKilled;

    @Column(name = "control_wards_placed")
    private Integer controlWardsPlaced;

    @Column(name = "total_time_spent_dead")
    private Integer totalTimeSpentDead;

    @Column(name = "is_win")
    private Boolean isWin;

    @Column(name = "raw_json", columnDefinition = "text")
    private String rawJson;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public Integer getParticipantId() {
        return participantId;
    }

    public void setParticipantId(Integer participantId) {
        this.participantId = participantId;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getTeamPosition() {
        return teamPosition;
    }

    public void setTeamPosition(String teamPosition) {
        this.teamPosition = teamPosition;
    }

    public String getChampionName() {
        return championName;
    }

    public void setChampionName(String championName) {
        this.championName = championName;
    }

    public Integer getKills() {
        return kills;
    }

    public void setKills(Integer kills) {
        this.kills = kills;
    }

    public Integer getDeaths() {
        return deaths;
    }

    public void setDeaths(Integer deaths) {
        this.deaths = deaths;
    }

    public Integer getAssists() {
        return assists;
    }

    public void setAssists(Integer assists) {
        this.assists = assists;
    }

    public Integer getTotalCs() {
        return totalCs;
    }

    public void setTotalCs(Integer totalCs) {
        this.totalCs = totalCs;
    }

    public Integer getGoldEarned() {
        return goldEarned;
    }

    public void setGoldEarned(Integer goldEarned) {
        this.goldEarned = goldEarned;
    }

    public Integer getDamageToChampions() {
        return damageToChampions;
    }

    public void setDamageToChampions(Integer damageToChampions) {
        this.damageToChampions = damageToChampions;
    }

    public Integer getVisionScore() {
        return visionScore;
    }

    public void setVisionScore(Integer visionScore) {
        this.visionScore = visionScore;
    }

    public Integer getWardsPlaced() {
        return wardsPlaced;
    }

    public void setWardsPlaced(Integer wardsPlaced) {
        this.wardsPlaced = wardsPlaced;
    }

    public Integer getWardsKilled() {
        return wardsKilled;
    }

    public void setWardsKilled(Integer wardsKilled) {
        this.wardsKilled = wardsKilled;
    }

    public Integer getControlWardsPlaced() {
        return controlWardsPlaced;
    }

    public void setControlWardsPlaced(Integer controlWardsPlaced) {
        this.controlWardsPlaced = controlWardsPlaced;
    }

    public Integer getTotalTimeSpentDead() {
        return totalTimeSpentDead;
    }

    public void setTotalTimeSpentDead(Integer totalTimeSpentDead) {
        this.totalTimeSpentDead = totalTimeSpentDead;
    }

    public Boolean getIsWin() {
        return isWin;
    }

    public void setIsWin(Boolean isWin) {
        this.isWin = isWin;
    }

    public String getRawJson() {
        return rawJson;
    }

    public void setRawJson(String rawJson) {
        this.rawJson = rawJson;
    }

    public OffsetDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(OffsetDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}