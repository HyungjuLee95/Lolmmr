package com.example.mmrtest.dto;

public class SummonerDTO {
    private String id; // encrypted id (티어 조회용)
    private String puuid;
    private String name;
    private int summonerLevel;

    public static class RankInfo {
        private String tier;
        private String rank;
        private int leaguePoints;

        public RankInfo(String tier, String rank, int leaguePoints) {
            this.tier = tier;
            this.rank = rank;
            this.leaguePoints = leaguePoints;
        }

        public RankInfo() {
            this.tier = "UNRANKED";
            this.rank = "";
            this.leaguePoints = 0;
        }

        public String getTier() {
            return tier;
        }

        public void setTier(String tier) {
            this.tier = tier;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
        }

        public int getLeaguePoints() {
            return leaguePoints;
        }

        public void setLeaguePoints(int leaguePoints) {
            this.leaguePoints = leaguePoints;
        }
    }

    private RankInfo soloRank = new RankInfo();
    private RankInfo flexRank = new RankInfo();

    // 기존 호환용 (혹은 삭제 예정, 일단 Deprecated 처리 권장)
    @Deprecated
    private String tier;
    @Deprecated
    private String rank;
    @Deprecated
    private int leaguePoints;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPuuid() {
        return puuid;
    }

    public void setPuuid(String puuid) {
        this.puuid = puuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSummonerLevel() {
        return summonerLevel;
    }

    public void setSummonerLevel(int summonerLevel) {
        this.summonerLevel = summonerLevel;
    }

    public RankInfo getSoloRank() {
        return soloRank;
    }

    public void setSoloRank(RankInfo soloRank) {
        this.soloRank = soloRank;
    }

    public RankInfo getFlexRank() {
        return flexRank;
    }

    public void setFlexRank(RankInfo flexRank) {
        this.flexRank = flexRank;
    }

    @Deprecated
    public String getTier() {
        return tier;
    }

    @Deprecated
    public void setTier(String tier) {
        this.tier = tier;
    }

    @Deprecated
    public String getRank() {
        return rank;
    }

    @Deprecated
    public void setRank(String rank) {
        this.rank = rank;
    }

    @Deprecated
    public int getLeaguePoints() {
        return leaguePoints;
    }

    @Deprecated
    public void setLeaguePoints(int leaguePoints) {
        this.leaguePoints = leaguePoints;
    }
}
