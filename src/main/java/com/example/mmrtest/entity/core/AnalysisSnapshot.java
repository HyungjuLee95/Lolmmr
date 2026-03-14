package com.example.mmrtest.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "analysis_snapshot")
@IdClass(AnalysisSnapshotId.class)
public class AnalysisSnapshot {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Id
    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Id
    @Column(name = "bucket_minutes", nullable = false)
    private Integer bucketMinutes;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public Integer getBucketMinutes() { return bucketMinutes; }
    public void setBucketMinutes(Integer bucketMinutes) { this.bucketMinutes = bucketMinutes; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public OffsetDateTime getComputedAt() { return computedAt; }
    public void setComputedAt(OffsetDateTime computedAt) { this.computedAt = computedAt; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(OffsetDateTime expiresAt) { this.expiresAt = expiresAt; }
}
