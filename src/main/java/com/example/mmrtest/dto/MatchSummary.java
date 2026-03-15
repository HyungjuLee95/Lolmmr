package com.example.mmrtest.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MatchSummary implements Serializable {
    private static final long serialVersionUID = 1L;
    // ===== 기존 핵심 식별 정보 =====
    private String matchId;
    private MatchResultType resultType;

    // ===== 기존 기본 전적 정보 =====
    private int kills;
    private int deaths;
    private int assists;
    private String championName;

    private List<Integer> items = new ArrayList<>();
    private List<String> teamMembers = new ArrayList<>();
    private List<String> teamChamps = new ArrayList<>();

    private int gameDurationMinutes;
    private int spell1Id;
    private int spell2Id;
    private int mainRuneId;
    private int subRuneId;
    private int totalCs;
    private int goldEarned;
    private int queueId;
    private long gameEndTimeStamp;
    private int performanceScore;
    private String teamPosition;

    private boolean invalid;
    private boolean countedGame;
    private boolean remake;
    private boolean win;
    private String displayResult;
    private boolean loss;

    // ===== v1 확장: 참가자/팀 기본 식별 =====
    private int participantId;
    private int teamId;
    private String riotId;

    // ===== v1 확장: 개인 상세 스탯 =====
    private int championLevel;
    private int visionScore;
    private int controlWardsPlaced;
    private int wardsPlaced;
    private int wardsKilled;
    private int totalTimeSpentDead;

    private int damageToChampions;
    private int damageToObjectives;
    private int damageToTurrets;

    // ===== v1 확장: 팀 총합 스탯 =====
    private int teamKills;
    private int teamGoldEarned;
    private int teamDamageToChampions;

    // ===== v1 확장: 15분 지표 =====
    private int goldDiff15;
    private int csDiff15;
    private int xpDiff15;

    // ===== v1 확장: 파생 지표 =====
    private double killParticipation;
    private double damageShare;
    private double damageConversion;
    private double visionPerMinute;
    private double goldPerMinute;
    private double csPerMinute;
    private double timeAliveRatio;

    // ===== v1 확장: 오브젝트/패배유발 =====
    private int objectiveParticipationScore;
    private int throwDeathPenalty;
    private boolean leaver;

    // ===== v1 확장: 점수 계산 결과 =====
    private int baseDelta;
    private int performanceDelta;
    private int finalDelta;

    private double perfIndex;
    private double growthScore;
    private double teamplayScore;
    private double efficiencyScore;
    private double survivalScore;

    private String scoreTier;

    public MatchSummary() {
    }

    // ===== 기존 getter/setter =====

    public String getMatchId() {
        return matchId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public MatchResultType getResultType() {
        return resultType;
    }

    public void setResultType(MatchResultType resultType) {
        this.resultType = resultType;
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public int getAssists() {
        return assists;
    }

    public void setAssists(int assists) {
        this.assists = assists;
    }

    public String getChampionName() {
        return championName;
    }

    public void setChampionName(String championName) {
        this.championName = championName;
    }

    public List<Integer> getItems() {
        return items;
    }

    public void setItems(List<Integer> items) {
        this.items = items == null ? new ArrayList<>() : items;
    }

    public List<String> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<String> teamMembers) {
        this.teamMembers = teamMembers == null ? new ArrayList<>() : teamMembers;
    }

    public List<String> getTeamChamps() {
        return teamChamps;
    }

    public void setTeamChamps(List<String> teamChamps) {
        this.teamChamps = teamChamps == null ? new ArrayList<>() : teamChamps;
    }

    public int getGameDurationMinutes() {
        return gameDurationMinutes;
    }

    public void setGameDurationMinutes(int gameDurationMinutes) {
        this.gameDurationMinutes = gameDurationMinutes;
    }

    public int getSpell1Id() {
        return spell1Id;
    }

    public void setSpell1Id(int spell1Id) {
        this.spell1Id = spell1Id;
    }

    public int getSpell2Id() {
        return spell2Id;
    }

    public void setSpell2Id(int spell2Id) {
        this.spell2Id = spell2Id;
    }

    public int getMainRuneId() {
        return mainRuneId;
    }

    public void setMainRuneId(int mainRuneId) {
        this.mainRuneId = mainRuneId;
    }

    public int getSubRuneId() {
        return subRuneId;
    }

    public void setSubRuneId(int subRuneId) {
        this.subRuneId = subRuneId;
    }

    public int getTotalCs() {
        return totalCs;
    }

    public void setTotalCs(int totalCs) {
        this.totalCs = totalCs;
    }

    public int getGoldEarned() {
        return goldEarned;
    }

    public void setGoldEarned(int goldEarned) {
        this.goldEarned = goldEarned;
    }

    public int getQueueId() {
        return queueId;
    }

    public void setQueueId(int queueId) {
        this.queueId = queueId;
    }

    public long getGameEndTimeStamp() {
        return gameEndTimeStamp;
    }

    public void setGameEndTimeStamp(long gameEndTimeStamp) {
        this.gameEndTimeStamp = gameEndTimeStamp;
    }

    public int getPerformanceScore() {
        return performanceScore;
    }

    public void setPerformanceScore(int performanceScore) {
        this.performanceScore = performanceScore;
    }

    public String getTeamPosition() {
        return teamPosition;
    }

    public void setTeamPosition(String teamPosition) {
        this.teamPosition = teamPosition;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public boolean isCountedGame() {
        return countedGame;
    }

    public void setCountedGame(boolean countedGame) {
        this.countedGame = countedGame;
    }

    public boolean isRemake() {
        return remake;
    }

    public void setRemake(boolean remake) {
        this.remake = remake;
    }

    public boolean isWin() {
        return win;
    }

    public void setWin(boolean win) {
        this.win = win;
    }

    public String getDisplayResult() {
        return displayResult;
    }

    public void setDisplayResult(String displayResult) {
        this.displayResult = displayResult;
    }

    public boolean isLoss() {
        return loss;
    }

    public void setLoss(boolean loss) {
        this.loss = loss;
    }

    // ===== v1 확장 getter/setter =====

    public int getParticipantId() {
        return participantId;
    }

    public void setParticipantId(int participantId) {
        this.participantId = participantId;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setTeamId(int teamId) {
        this.teamId = teamId;
    }

    public String getRiotId() {
        return riotId;
    }

    public void setRiotId(String riotId) {
        this.riotId = riotId;
    }

    public int getChampionLevel() {
        return championLevel;
    }

    public void setChampionLevel(int championLevel) {
        this.championLevel = championLevel;
    }

    public int getVisionScore() {
        return visionScore;
    }

    public void setVisionScore(int visionScore) {
        this.visionScore = visionScore;
    }

    public int getControlWardsPlaced() {
        return controlWardsPlaced;
    }

    public void setControlWardsPlaced(int controlWardsPlaced) {
        this.controlWardsPlaced = controlWardsPlaced;
    }

    public int getWardsPlaced() {
        return wardsPlaced;
    }

    public void setWardsPlaced(int wardsPlaced) {
        this.wardsPlaced = wardsPlaced;
    }

    public int getWardsKilled() {
        return wardsKilled;
    }

    public void setWardsKilled(int wardsKilled) {
        this.wardsKilled = wardsKilled;
    }

    public int getTotalTimeSpentDead() {
        return totalTimeSpentDead;
    }

    public void setTotalTimeSpentDead(int totalTimeSpentDead) {
        this.totalTimeSpentDead = totalTimeSpentDead;
    }

    public int getDamageToChampions() {
        return damageToChampions;
    }

    public void setDamageToChampions(int damageToChampions) {
        this.damageToChampions = damageToChampions;
    }

    public int getDamageToObjectives() {
        return damageToObjectives;
    }

    public void setDamageToObjectives(int damageToObjectives) {
        this.damageToObjectives = damageToObjectives;
    }

    public int getDamageToTurrets() {
        return damageToTurrets;
    }

    public void setDamageToTurrets(int damageToTurrets) {
        this.damageToTurrets = damageToTurrets;
    }

    public int getTeamKills() {
        return teamKills;
    }

    public void setTeamKills(int teamKills) {
        this.teamKills = teamKills;
    }

    public int getTeamGoldEarned() {
        return teamGoldEarned;
    }

    public void setTeamGoldEarned(int teamGoldEarned) {
        this.teamGoldEarned = teamGoldEarned;
    }

    public int getTeamDamageToChampions() {
        return teamDamageToChampions;
    }

    public void setTeamDamageToChampions(int teamDamageToChampions) {
        this.teamDamageToChampions = teamDamageToChampions;
    }

    public int getGoldDiff15() {
        return goldDiff15;
    }

    public void setGoldDiff15(int goldDiff15) {
        this.goldDiff15 = goldDiff15;
    }

    public int getCsDiff15() {
        return csDiff15;
    }

    public void setCsDiff15(int csDiff15) {
        this.csDiff15 = csDiff15;
    }

    public int getXpDiff15() {
        return xpDiff15;
    }

    public void setXpDiff15(int xpDiff15) {
        this.xpDiff15 = xpDiff15;
    }

    public double getKillParticipation() {
        return killParticipation;
    }

    public void setKillParticipation(double killParticipation) {
        this.killParticipation = killParticipation;
    }

    public double getDamageShare() {
        return damageShare;
    }

    public void setDamageShare(double damageShare) {
        this.damageShare = damageShare;
    }

    public double getDamageConversion() {
        return damageConversion;
    }

    public void setDamageConversion(double damageConversion) {
        this.damageConversion = damageConversion;
    }

    public double getVisionPerMinute() {
        return visionPerMinute;
    }

    public void setVisionPerMinute(double visionPerMinute) {
        this.visionPerMinute = visionPerMinute;
    }

    public double getGoldPerMinute() {
        return goldPerMinute;
    }

    public void setGoldPerMinute(double goldPerMinute) {
        this.goldPerMinute = goldPerMinute;
    }

    public double getCsPerMinute() {
        return csPerMinute;
    }

    public void setCsPerMinute(double csPerMinute) {
        this.csPerMinute = csPerMinute;
    }

    public double getTimeAliveRatio() {
        return timeAliveRatio;
    }

    public void setTimeAliveRatio(double timeAliveRatio) {
        this.timeAliveRatio = timeAliveRatio;
    }

    public int getObjectiveParticipationScore() {
        return objectiveParticipationScore;
    }

    public void setObjectiveParticipationScore(int objectiveParticipationScore) {
        this.objectiveParticipationScore = objectiveParticipationScore;
    }

    public int getThrowDeathPenalty() {
        return throwDeathPenalty;
    }

    public void setThrowDeathPenalty(int throwDeathPenalty) {
        this.throwDeathPenalty = throwDeathPenalty;
    }

    public boolean isLeaver() {
        return leaver;
    }

    public void setLeaver(boolean leaver) {
        this.leaver = leaver;
    }

    public int getBaseDelta() {
        return baseDelta;
    }

    public void setBaseDelta(int baseDelta) {
        this.baseDelta = baseDelta;
    }

    public int getPerformanceDelta() {
        return performanceDelta;
    }

    public void setPerformanceDelta(int performanceDelta) {
        this.performanceDelta = performanceDelta;
    }

    public int getFinalDelta() {
        return finalDelta;
    }

    public void setFinalDelta(int finalDelta) {
        this.finalDelta = finalDelta;
    }

    public double getPerfIndex() {
        return perfIndex;
    }

    public void setPerfIndex(double perfIndex) {
        this.perfIndex = perfIndex;
    }

    public double getGrowthScore() {
        return growthScore;
    }

    public void setGrowthScore(double growthScore) {
        this.growthScore = growthScore;
    }

    public double getTeamplayScore() {
        return teamplayScore;
    }

    public void setTeamplayScore(double teamplayScore) {
        this.teamplayScore = teamplayScore;
    }

    public double getEfficiencyScore() {
        return efficiencyScore;
    }

    public void setEfficiencyScore(double efficiencyScore) {
        this.efficiencyScore = efficiencyScore;
    }

    public double getSurvivalScore() {
        return survivalScore;
    }

    public void setSurvivalScore(double survivalScore) {
        this.survivalScore = survivalScore;
    }

    public String getScoreTier() {
        return scoreTier;
    }

    public void setScoreTier(String scoreTier) {
        this.scoreTier = scoreTier;
    }
}