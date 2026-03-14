package com.example.mmrtest.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "timeline_summary")
@IdClass(TimelineSummaryId.class)
public class TimelineSummary {

    @Id
    @Column(name = "match_id", nullable = false)
    private String matchId;

    @Id
    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Column(name = "gold_diff_15")
    private Integer goldDiff15;

    @Column(name = "cs_diff_15")
    private Integer csDiff15;

    @Column(name = "xp_diff_15")
    private Integer xpDiff15;

    @Column(name = "objective_participation_score")
    private Integer objectiveParticipationScore;

    @Column(name = "throw_death_penalty")
    private Integer throwDeathPenalty;

    @Column(name = "timeline_fetched_at", nullable = false)
    private OffsetDateTime timelineFetchedAt;

    public String getMatchId() { return matchId; }
    public void setMatchId(String matchId) { this.matchId = matchId; }
    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public Integer getGoldDiff15() { return goldDiff15; }
    public void setGoldDiff15(Integer goldDiff15) { this.goldDiff15 = goldDiff15; }
    public Integer getCsDiff15() { return csDiff15; }
    public void setCsDiff15(Integer csDiff15) { this.csDiff15 = csDiff15; }
    public Integer getXpDiff15() { return xpDiff15; }
    public void setXpDiff15(Integer xpDiff15) { this.xpDiff15 = xpDiff15; }
    public Integer getObjectiveParticipationScore() { return objectiveParticipationScore; }
    public void setObjectiveParticipationScore(Integer objectiveParticipationScore) { this.objectiveParticipationScore = objectiveParticipationScore; }
    public Integer getThrowDeathPenalty() { return throwDeathPenalty; }
    public void setThrowDeathPenalty(Integer throwDeathPenalty) { this.throwDeathPenalty = throwDeathPenalty; }
    public OffsetDateTime getTimelineFetchedAt() { return timelineFetchedAt; }
    public void setTimelineFetchedAt(OffsetDateTime timelineFetchedAt) { this.timelineFetchedAt = timelineFetchedAt; }
}
