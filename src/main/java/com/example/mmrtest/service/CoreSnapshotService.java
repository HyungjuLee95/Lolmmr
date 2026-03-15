package com.example.mmrtest.service;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.entity.core.AnalysisSnapshot;
import com.example.mmrtest.entity.core.MatchCore;
import com.example.mmrtest.entity.core.MatchParticipant;
import com.example.mmrtest.entity.core.RankSnapshot;
import com.example.mmrtest.entity.core.SummonerProfile;
import com.example.mmrtest.entity.core.TimelineSummary;
import com.example.mmrtest.repository.core.AnalysisSnapshotRepository;
import com.example.mmrtest.repository.core.MatchCoreRepository;
import com.example.mmrtest.repository.core.MatchParticipantRepository;
import com.example.mmrtest.repository.core.RankSnapshotRepository;
import com.example.mmrtest.repository.core.SummonerProfileRepository;
import com.example.mmrtest.repository.core.TimelineSummaryRepository;

@Service
@Transactional
public class CoreSnapshotService {

    private final SummonerProfileRepository summonerProfileRepository;
    private final RankSnapshotRepository rankSnapshotRepository;
    private final MatchCoreRepository matchCoreRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final TimelineSummaryRepository timelineSummaryRepository;
    private final AnalysisSnapshotRepository analysisSnapshotRepository;

    public CoreSnapshotService(
            SummonerProfileRepository summonerProfileRepository,
            RankSnapshotRepository rankSnapshotRepository,
            MatchCoreRepository matchCoreRepository,
            MatchParticipantRepository matchParticipantRepository,
            TimelineSummaryRepository timelineSummaryRepository,
            AnalysisSnapshotRepository analysisSnapshotRepository
    ) {
        this.summonerProfileRepository = summonerProfileRepository;
        this.rankSnapshotRepository = rankSnapshotRepository;
        this.matchCoreRepository = matchCoreRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.timelineSummaryRepository = timelineSummaryRepository;
        this.analysisSnapshotRepository = analysisSnapshotRepository;
    }

    public void saveSummonerSnapshot(String gameName, String tagLine, SummonerDTO summoner) {
        if (summoner == null || !StringUtils.hasText(summoner.getPuuid())) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();

        SummonerProfile profile = summonerProfileRepository
                .findById(summoner.getPuuid())
                .orElseGet(SummonerProfile::new);

        profile.setPuuid(summoner.getPuuid());
        profile.setGameName(StringUtils.hasText(gameName) ? gameName : safeString(summoner.getName()));
        profile.setTagLine(StringUtils.hasText(tagLine) ? tagLine : "KR1");
        profile.setSummonerId(summoner.getId());
        profile.setProfileIconId(summoner.getProfileIconId());
        profile.setSummonerLevel(summoner.getSummonerLevel());
        profile.setLastProfileSyncAt(now);

        summonerProfileRepository.save(profile);

        saveRankSnapshot(summoner.getPuuid(), "RANKED_SOLO_5x5", summoner.getSoloRank(), now);
        saveRankSnapshot(summoner.getPuuid(), "RANKED_FLEX_SR", summoner.getFlexRank(), now);
    }

    public void saveMatchSnapshots(String puuid, List<MatchSummary> matches) {
        if (!StringUtils.hasText(puuid) || matches == null || matches.isEmpty()) {
            return;
        }

        for (MatchSummary match : matches) {
            if (match == null || !StringUtils.hasText(match.getMatchId())) {
                continue;
            }

            saveMatchCore(match);
            saveMatchParticipant(puuid, match);
            saveTimelineSummary(puuid, match);
        }
    }

    public void saveAllSnapshots(
            String gameName,
            String tagLine,
            SummonerDTO summoner,
            List<MatchSummary> soloMatches,
            List<MatchSummary> flexMatches
    ) {
        saveSummonerSnapshot(gameName, tagLine, summoner);
        saveMatchSnapshots(summoner != null ? summoner.getPuuid() : null, safeList(soloMatches));
        saveMatchSnapshots(summoner != null ? summoner.getPuuid() : null, safeList(flexMatches));
    }

    public void saveAnalysisSnapshot(
            String matchId,
            String puuid,
            Integer bucketMinutes,
            String payload,
            OffsetDateTime expiresAt
    ) {
        if (!StringUtils.hasText(matchId)
                || !StringUtils.hasText(puuid)
                || bucketMinutes == null
                || !StringUtils.hasText(payload)) {
            return;
        }

        AnalysisSnapshot entity = new AnalysisSnapshot();
        entity.setMatchId(matchId);
        entity.setPuuid(puuid);
        entity.setBucketMinutes(bucketMinutes);
        entity.setPayload(payload);
        entity.setComputedAt(OffsetDateTime.now());
        entity.setExpiresAt(expiresAt);

        analysisSnapshotRepository.save(entity);
    }

    private void saveRankSnapshot(
            String puuid,
            String queueType,
            SummonerDTO.RankInfo rankInfo,
            OffsetDateTime fetchedAt
    ) {
        if (!StringUtils.hasText(puuid) || !StringUtils.hasText(queueType)) {
            return;
        }

        RankSnapshot snapshot = new RankSnapshot();
        snapshot.setPuuid(puuid);
        snapshot.setQueueType(queueType);

        if (rankInfo != null && StringUtils.hasText(rankInfo.getTier())
                && !"UNRANKED".equalsIgnoreCase(rankInfo.getTier())) {
            snapshot.setTier(rankInfo.getTier());
            snapshot.setRank(rankInfo.getRank());
            snapshot.setLeaguePoints(rankInfo.getLeaguePoints());
        } else {
            snapshot.setTier(null);
            snapshot.setRank(null);
            snapshot.setLeaguePoints(null);
        }

        // 현재 SummonerDTO에는 wins/losses가 없어서 v1에서는 비워 둔다.
        snapshot.setWins(null);
        snapshot.setLosses(null);
        snapshot.setFetchedAt(fetchedAt);

        rankSnapshotRepository.save(snapshot);
    }

    private void saveMatchCore(MatchSummary match) {
        MatchCore core = new MatchCore();
        core.setMatchId(match.getMatchId());
        core.setQueueId(match.getQueueId());
        core.setGameDurationSeconds(Math.max(match.getGameDurationMinutes(), 0) * 60);
        core.setGameEndTimestamp(match.getGameEndTimeStamp());
        core.setGameVersion(null);
        core.setFetchedAt(OffsetDateTime.now());

        matchCoreRepository.save(core);
    }

    private void saveMatchParticipant(String puuid, MatchSummary match) {
        MatchParticipant participant = new MatchParticipant();
        participant.setMatchId(match.getMatchId());
        participant.setPuuid(puuid);
        participant.setParticipantId(match.getParticipantId());
        participant.setTeamId(match.getTeamId());
        participant.setTeamPosition(match.getTeamPosition());
        participant.setChampionName(match.getChampionName());
        participant.setKills(match.getKills());
        participant.setDeaths(match.getDeaths());
        participant.setAssists(match.getAssists());
        participant.setTotalCs(match.getTotalCs());
        participant.setGoldEarned(match.getGoldEarned());
        participant.setDamageToChampions(match.getDamageToChampions());
        participant.setVisionScore(match.getVisionScore());
        participant.setWardsPlaced(match.getWardsPlaced());
        participant.setWardsKilled(match.getWardsKilled());
        participant.setControlWardsPlaced(match.getControlWardsPlaced());
        participant.setTotalTimeSpentDead(match.getTotalTimeSpentDead());
        participant.setIsWin(match.isWin());
        participant.setRawJson(null);
        participant.setFetchedAt(OffsetDateTime.now());

        matchParticipantRepository.save(participant);
    }

    private void saveTimelineSummary(String puuid, MatchSummary match) {
        TimelineSummary timeline = new TimelineSummary();
        timeline.setMatchId(match.getMatchId());
        timeline.setPuuid(puuid);
        timeline.setGoldDiff15(match.getGoldDiff15());
        timeline.setCsDiff15(match.getCsDiff15());
        timeline.setXpDiff15(match.getXpDiff15());
        timeline.setObjectiveParticipationScore(match.getObjectiveParticipationScore());
        timeline.setThrowDeathPenalty(match.getThrowDeathPenalty());
        timeline.setTimelineFetchedAt(OffsetDateTime.now());

        timelineSummaryRepository.save(timeline);
    }

    private List<MatchSummary> safeList(List<MatchSummary> matches) {
        return matches == null ? Collections.emptyList() : matches;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}