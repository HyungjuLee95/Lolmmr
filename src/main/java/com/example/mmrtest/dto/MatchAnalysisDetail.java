package com.example.mmrtest.dto;

import java.util.ArrayList;
import java.util.List;

public class MatchAnalysisDetail {
    private String matchId;
    private String puuid;
    private MatchResultType resultType = MatchResultType.INVALID;

    private MatchSummary summary;

    private MatchTeamOverview blueTeamSummary = new MatchTeamOverview();
    private MatchTeamOverview redTeamSummary = new MatchTeamOverview();

    private List<MatchParticipantOverview> blueTeamPlayers = new ArrayList<>();
    private List<MatchParticipantOverview> redTeamPlayers = new ArrayList<>();

    private LaneOpponentComparison laneComparison;
    private List<TimelineBucket> timelineBuckets = new ArrayList<>();
    private List<MetricCard> metricCards = new ArrayList<>();
    private List<CoachingComment> coachingComments = new ArrayList<>();

    private int bucketMinutes = 3;

    public MatchAnalysisDetail() {
    }

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

    public MatchResultType getResultType() {
        return resultType;
    }

    public void setResultType(MatchResultType resultType) {
        this.resultType = resultType == null ? MatchResultType.INVALID : resultType;
    }

    public MatchSummary getSummary() {
        return summary;
    }

    public void setSummary(MatchSummary summary) {
        this.summary = summary;
    }

    public MatchTeamOverview getBlueTeamSummary() {
        return blueTeamSummary;
    }

    public void setBlueTeamSummary(MatchTeamOverview blueTeamSummary) {
        this.blueTeamSummary = blueTeamSummary == null ? new MatchTeamOverview() : blueTeamSummary;
    }

    public MatchTeamOverview getRedTeamSummary() {
        return redTeamSummary;
    }

    public void setRedTeamSummary(MatchTeamOverview redTeamSummary) {
        this.redTeamSummary = redTeamSummary == null ? new MatchTeamOverview() : redTeamSummary;
    }

    public List<MatchParticipantOverview> getBlueTeamPlayers() {
        return blueTeamPlayers;
    }

    public void setBlueTeamPlayers(List<MatchParticipantOverview> blueTeamPlayers) {
        this.blueTeamPlayers = blueTeamPlayers == null ? new ArrayList<>() : blueTeamPlayers;
    }

    public List<MatchParticipantOverview> getRedTeamPlayers() {
        return redTeamPlayers;
    }

    public void setRedTeamPlayers(List<MatchParticipantOverview> redTeamPlayers) {
        this.redTeamPlayers = redTeamPlayers == null ? new ArrayList<>() : redTeamPlayers;
    }

    public LaneOpponentComparison getLaneComparison() {
        return laneComparison;
    }

    public void setLaneComparison(LaneOpponentComparison laneComparison) {
        this.laneComparison = laneComparison;
    }

    public List<TimelineBucket> getTimelineBuckets() {
        return timelineBuckets;
    }

    public void setTimelineBuckets(List<TimelineBucket> timelineBuckets) {
        this.timelineBuckets = timelineBuckets == null ? new ArrayList<>() : timelineBuckets;
    }

    public List<MetricCard> getMetricCards() {
        return metricCards;
    }

    public void setMetricCards(List<MetricCard> metricCards) {
        this.metricCards = metricCards == null ? new ArrayList<>() : metricCards;
    }

    public List<CoachingComment> getCoachingComments() {
        return coachingComments;
    }

    public void setCoachingComments(List<CoachingComment> coachingComments) {
        this.coachingComments = coachingComments == null ? new ArrayList<>() : coachingComments;
    }

    public int getBucketMinutes() {
        return bucketMinutes;
    }

    public void setBucketMinutes(int bucketMinutes) {
        this.bucketMinutes = bucketMinutes <= 0 ? 3 : bucketMinutes;
    }
}