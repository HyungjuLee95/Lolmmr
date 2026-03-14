package com.example.mmrtest.entity.core;

import java.io.Serializable;
import java.util.Objects;

public class TimelineSummaryId implements Serializable {
    private String matchId;
    private String puuid;

    public TimelineSummaryId() {}

    public TimelineSummaryId(String matchId, String puuid) {
        this.matchId = matchId;
        this.puuid = puuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TimelineSummaryId that)) return false;
        return Objects.equals(matchId, that.matchId) && Objects.equals(puuid, that.puuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, puuid);
    }
}
