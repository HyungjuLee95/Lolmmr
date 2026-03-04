package com.example.mmrtest.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummonerDTO {
    private String id; // encrypted id (티어 조회용)
    private String puuid;
    private String name;
    private int summonerLevel;

    // 랭크 정보를 담을 내부 클래스
    @Getter
    @Setter
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
}
