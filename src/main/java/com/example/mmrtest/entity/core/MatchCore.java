package com.example.mmrtest.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "match_core")
public class MatchCore {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Column(name = "queue_id", nullable = false)
    private Integer queueId;

    @Column(name = "game_duration_seconds")
    private Integer gameDurationSeconds;

    @Column(name = "game_end_timestamp")
    private Long gameEndTimestamp;

    @Column(name = "game_version")
    private String gameVersion;

    @Column(name = "fetched_at", nullable = false)
    private OffsetDateTime fetchedAt;

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public Integer getQueueId() { return queueId; }
    public void setQueueId(Integer queueId) { this.queueId = queueId; }
    public Integer getGameDurationSeconds() { return gameDurationSeconds; }
    public void setGameDurationSeconds(Integer gameDurationSeconds) { this.gameDurationSeconds = gameDurationSeconds; }
    public Long getGameEndTimestamp() { return gameEndTimestamp; }
    public void setGameEndTimestamp(Long gameEndTimestamp) { this.gameEndTimestamp = gameEndTimestamp; }
    public String getGameVersion() { return gameVersion; }
    public void setGameVersion(String gameVersion) { this.gameVersion = gameVersion; }
    public OffsetDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(OffsetDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}
