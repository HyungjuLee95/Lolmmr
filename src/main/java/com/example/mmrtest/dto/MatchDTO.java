package com.example.mmrtest.dto;

import java.util.List;

public class MatchDTO {
    private Metadata metadata;
    private Info info;

    public Metadata getMetadata() {
        return metadata;
    }

    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public static class Metadata {
        private String matchId;

        public String getMatchId() {
            return matchId;
        }

        public void setMatchId(String matchId) {
            this.matchId = matchId;
        }
    }

    public static class Info {
        private List<Participant> participants;
        private String gameMode;

        public List<Participant> getParticipants() {
            return participants;
        }

        public void setParticipants(List<Participant> participants) {
            this.participants = participants;
        }

        public String getGameMode() {
            return gameMode;
        }

        public void setGameMode(String gameMode) {
            this.gameMode = gameMode;
        }
    }

    public static class Participant {
        private String puuid;
        private String summonerName;
        private int win;
        private boolean winBoolean;

        public String getPuuid() {
            return puuid;
        }

        public void setPuuid(String puuid) {
            this.puuid = puuid;
        }

        public String getSummonerName() {
            return summonerName;
        }

        public void setSummonerName(String summonerName) {
            this.summonerName = summonerName;
        }

        public int getWin() {
            return win;
        }

        public void setWin(int win) {
            this.win = win;
        }

        public boolean isWinBoolean() {
            return winBoolean;
        }

        public void setWinBoolean(boolean winBoolean) {
            this.winBoolean = winBoolean;
        }
    }
}
