package com.example.mmrtest.entity.core;

import java.io.Serializable;
import java.util.Objects;

public class MatchParticipantId implements Serializable {
    private String matchId;
    private String puuid;

    public MatchParticipantId() {}

    public MatchParticipantId(String matchId, String puuid) {
        this.matchId = matchId;
        this.puuid = puuid;
    }

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchParticipantId that)) return false;
        return Objects.equals(matchId, that.matchId) && Objects.equals(puuid, that.puuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matchId, puuid);
    }
}
