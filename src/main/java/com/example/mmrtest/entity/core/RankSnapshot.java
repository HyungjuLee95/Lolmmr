package com.example.mmrtest.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "rank_snapshot")
@IdClass(RankSnapshotId.class)
public class RankSnapshot {

    @Id
    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Id
    @Column(name = "queue_type", nullable = false)
    private String queueType;

    @Column(name = "tier")
    private String tier;

    @Column(name = "rank")
    private String rank;

    @Column(name = "league_points")
    private Integer leaguePoints;

    @Column(name = "wins")
    private Integer wins;

    @Column(name = "losses")
    private Integer losses;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public String getQueueType() { return queueType; }
    public void setQueueType(String queueType) { this.queueType = queueType; }
    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }
    public String getRank() { return rank; }
    public void setRank(String rank) { this.rank = rank; }
    public Integer getLeaguePoints() { return leaguePoints; }
    public void setLeaguePoints(Integer leaguePoints) { this.leaguePoints = leaguePoints; }
    public Integer getWins() { return wins; }
    public void setWins(Integer wins) { this.wins = wins; }
    public Integer getLosses() { return losses; }
    public void setLosses(Integer losses) { this.losses = losses; }
    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}
