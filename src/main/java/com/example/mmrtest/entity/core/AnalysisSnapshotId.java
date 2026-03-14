package com.example.mmrtest.entity.core;

import java.io.Serializable;
import java.util.Objects;

public class AnalysisSnapshotId implements Serializable {
    private String matchId;
    private String puuid;
    private Integer bucketMinutes;

    public AnalysisSnapshotId() {}

    public AnalysisSnapshotId(String matchId, String puuid, Integer bucketMinutes) {
        this.matchId = matchId;
        this.puuid = puuid;
        this.bucketMinutes = bucketMinutes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisSnapshotId that)) return false;
        return Objects.equals(matchId, that.matchId)
                && Objects.equals(puuid, that.puuid)
                && Objects.equals(bucketMinutes, that.bucketMinutes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, puuid, bucketMinutes);
    }
}
