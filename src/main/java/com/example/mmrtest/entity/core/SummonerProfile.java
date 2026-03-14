package com.example.mmrtest.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "summoner_profile")
public class SummonerProfile {

    @Id
    @Column(name = "puuid", nullable = false)
    private String puuid;

    @Column(name = "game_name", nullable = false)
    private String gameName;

    @Column(name = "tag_line", nullable = false)
    private String tagLine;

    @Column(name = "summoner_id")
    private String summonerId;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    @Column(name = "summoner_level")
    private Integer summonerLevel;

    @Column(name = "last_profile_sync_at", nullable = false)
    private OffsetDateTime lastProfileSyncAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (lastProfileSyncAt == null) lastProfileSyncAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public String getPuuid() { return puuid; }
    public void setPuuid(String puuid) { this.puuid = puuid; }
    public String getGameName() { return gameName; }
    public void setGameName(String gameName) { this.gameName = gameName; }
    public String getTagLine() { return tagLine; }
    public void setTagLine(String tagLine) { this.tagLine = tagLine; }
    public String getSummonerId() { return summonerId; }
    public void setSummonerId(String summonerId) { this.summonerId = summonerId; }
    public Integer getProfileIconId() { return profileIconId; }
    public void setProfileIconId(Integer profileIconId) { this.profileIconId = profileIconId; }
    public Integer getSummonerLevel() { return summonerLevel; }
    public void setSummonerLevel(Integer summonerLevel) { this.summonerLevel = summonerLevel; }
    public OffsetDateTime getLastProfileSyncAt() { return lastProfileSyncAt; }
    public void setLastProfileSyncAt(OffsetDateTime lastProfileSyncAt) { this.lastProfileSyncAt = lastProfileSyncAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
