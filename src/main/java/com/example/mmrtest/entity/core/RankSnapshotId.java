package com.example.mmrtest.entity.core;

import java.io.Serializable;
import java.util.Objects;

public class RankSnapshotId implements Serializable {
    private String puuid;
    private String queueType;

    public RankSnapshotId() {}

    public RankSnapshotId(String puuid, String queueType) {
        this.puuid = puuid;
        this.queueType = queueType;
    }

    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public String getQueueType() { return queueType; }
    public void setQueueType(String queueType) { this.queueType = queueType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RankSnapshotId that)) return false;
        return Objects.equals(puuid, that.puuid) && Objects.equals(queueType, that.queueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(puuid, queueType);
    }
}
