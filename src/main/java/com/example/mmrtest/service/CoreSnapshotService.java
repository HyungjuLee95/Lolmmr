package com.example.mmrtest.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.mmrtest.dto.MatchResultType;
import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.entity.core.AnalysisSnapshot;
import com.example.mmrtest.entity.core.AnalysisSnapshotId;
import com.example.mmrtest.entity.core.MatchCore;
import com.example.mmrtest.entity.core.MatchParticipant;
import com.example.mmrtest.entity.core.MatchParticipantId;
import com.example.mmrtest.entity.core.RankSnapshot;
import com.example.mmrtest.entity.core.RankSnapshotId;
import com.example.mmrtest.entity.core.SummonerProfile;
import com.example.mmrtest.entity.core.TimelineSummary;
import com.example.mmrtest.entity.core.TimelineSummaryId;
import com.example.mmrtest.repository.core.AnalysisSnapshotRepository;
import com.example.mmrtest.repository.core.MatchCoreRepository;
import com.example.mmrtest.repository.core.MatchParticipantRepository;
import com.example.mmrtest.repository.core.RankSnapshotRepository;
import com.example.mmrtest.repository.core.SummonerProfileRepository;
import com.example.mmrtest.repository.core.TimelineSummaryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class CoreSnapshotService {

    private final SummonerProfileRepository summonerProfileRepository;
    private final RankSnapshotRepository rankSnapshotRepository;
    private final MatchCoreRepository matchCoreRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final TimelineSummaryRepository timelineSummaryRepository;
    private final AnalysisSnapshotRepository analysisSnapshotRepository;
    private final ObjectMapper objectMapper;

    public CoreSnapshotService(
            SummonerProfileRepository summonerProfileRepository,
            RankSnapshotRepository rankSnapshotRepository,
            MatchCoreRepository matchCoreRepository,
            MatchParticipantRepository matchParticipantRepository,
            TimelineSummaryRepository timelineSummaryRepository,
            AnalysisSnapshotRepository analysisSnapshotRepository,
            ObjectMapper objectMapper
    ) {
        this.summonerProfileRepository = summonerProfileRepository;
        this.rankSnapshotRepository = rankSnapshotRepository;
        this.matchCoreRepository = matchCoreRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.timelineSummaryRepository = timelineSummaryRepository;
        this.analysisSnapshotRepository = analysisSnapshotRepository;
        this.objectMapper = objectMapper;
    }

    public void saveSummonerSnapshot(String gameName, String tagLine, SummonerDTO summoner) {
        if (summoner == null || !StringUtils.hasText(summoner.getPuuid())) {
            return;
        }

        SummonerProfile profile = summonerProfileRepository
                .findById(summoner.getPuuid())
                .orElseGet(SummonerProfile::new);

        boolean changed = false;

        changed |= setIfDifferent(profile.getPuuid(), summoner.getPuuid(), profile::setPuuid);
        changed |= setIfDifferent(profile.getGameName(), StringUtils.hasText(gameName) ? gameName : safeString(summoner.getName()), profile::setGameName);
        changed |= setIfDifferent(profile.getTagLine(), StringUtils.hasText(tagLine) ? tagLine : "KR1", profile::setTagLine);
        changed |= setIfDifferent(profile.getSummonerId(), summoner.getId(), profile::setSummonerId);
        changed |= setIfDifferent(profile.getProfileIconId(), summoner.getProfileIconId(), profile::setProfileIconId);
        changed |= setIfDifferent(profile.getSummonerLevel(), summoner.getSummonerLevel(), profile::setSummonerLevel);

        if (changed || profile.getLastProfileSyncAt() == null) {
            profile.setLastProfileSyncAt(OffsetDateTime.now());
            summonerProfileRepository.save(profile);
        }

        saveRankSnapshot(summoner.getPuuid(), "RANKED_SOLO_5x5", summoner.getSoloRank());
        saveRankSnapshot(summoner.getPuuid(), "RANKED_FLEX_SR", summoner.getFlexRank());
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

        AnalysisSnapshotId id = new AnalysisSnapshotId(matchId, puuid, bucketMinutes);

        AnalysisSnapshot entity = analysisSnapshotRepository.findById(id)
                .orElseGet(AnalysisSnapshot::new);

        boolean changed = false;
        changed |= setIfDifferent(entity.getMatchId(), matchId, entity::setMatchId);
        changed |= setIfDifferent(entity.getPuuid(), puuid, entity::setPuuid);
        changed |= setIfDifferent(entity.getBucketMinutes(), bucketMinutes, entity::setBucketMinutes);
        changed |= setIfDifferent(entity.getPayload(), payload, entity::setPayload);
        changed |= setIfDifferent(entity.getExpiresAt(), expiresAt, entity::setExpiresAt);

        if (changed || entity.getComputedAt() == null) {
            entity.setComputedAt(OffsetDateTime.now());
            analysisSnapshotRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public <T> T findAnalysisSnapshot(
            String matchId,
            String puuid,
            Integer bucketMinutes,
            Class<T> responseType
    ) {
        if (!StringUtils.hasText(matchId)
                || !StringUtils.hasText(puuid)
                || bucketMinutes == null
                || responseType == null) {
            return null;
        }

        Optional<AnalysisSnapshot> snapshotOpt =
                analysisSnapshotRepository.findByMatchIdAndPuuidAndBucketMinutes(
                        matchId,
                        puuid,
                        bucketMinutes
                );

        if (snapshotOpt.isEmpty()) {
            return null;
        }

        AnalysisSnapshot snapshot = snapshotOpt.get();

        if (snapshot.getExpiresAt() != null && snapshot.getExpiresAt().isBefore(OffsetDateTime.now())) {
            return null;
        }

        if (!StringUtils.hasText(snapshot.getPayload())) {
            return null;
        }

        try {
            return objectMapper.readValue(snapshot.getPayload(), responseType);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Optional<SummonerProfile> findSummonerProfile(String gameName, String tagLine) {
        if (!StringUtils.hasText(gameName) || !StringUtils.hasText(tagLine)) {
            return Optional.empty();
        }
        return summonerProfileRepository.findByGameNameAndTagLine(gameName, tagLine);
    }

    @Transactional(readOnly = true)
    public SummonerDTO.RankInfo findLatestRank(String puuid, String queueType) {
        if (!StringUtils.hasText(puuid) || !StringUtils.hasText(queueType)) {
            return null;
        }

        Optional<RankSnapshot> snapshot =
                rankSnapshotRepository.findTopByPuuidAndQueueTypeOrderByFetchedAtDesc(puuid, queueType);

        if (snapshot.isEmpty() || !StringUtils.hasText(snapshot.get().getTier())) {
            return null;
        }

        RankSnapshot s = snapshot.get();
        return new SummonerDTO.RankInfo(
                s.getTier(),
                s.getRank(),
                s.getLeaguePoints() == null ? 0 : s.getLeaguePoints()
        );
    }

    @Transactional(readOnly = true)
    public List<MatchSummary> findRecentMatchSummaries(String puuid, Integer queueId, int limit) {
        if (!StringUtils.hasText(puuid) || limit <= 0) {
            return Collections.emptyList();
        }

        List<MatchParticipant> rows = matchParticipantRepository.findByPuuidOrderByFetchedAtDesc(puuid);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> seenMatchIds = new LinkedHashSet<>();
        List<MatchSummary> result = new ArrayList<>();

        for (MatchParticipant row : rows) {
            if (row == null || !StringUtils.hasText(row.getMatchId())) {
                continue;
            }

            String matchId = row.getMatchId();
            if (!seenMatchIds.add(matchId)) {
                continue;
            }

            Optional<MatchCore> coreOpt = matchCoreRepository.findById(matchId);
            if (coreOpt.isEmpty()) {
                continue;
            }

            MatchCore core = coreOpt.get();
            if (queueId != null && queueId > 0 && !queueId.equals(core.getQueueId())) {
                continue;
            }

            Optional<TimelineSummary> timelineOpt = timelineSummaryRepository.findById(
                    new TimelineSummaryId(matchId, puuid)
            );

            MatchSummary restored = restoreMatchSummary(row, core, timelineOpt.orElse(null));
            if (restored == null) {
                continue;
            }

            result.add(restored);
            if (result.size() >= limit) {
                break;
            }
        }

        result.sort(Comparator.comparingLong(MatchSummary::getGameEndTimeStamp).reversed());
        return result;
    }

    private MatchSummary restoreMatchSummary(
            MatchParticipant participant,
            MatchCore core,
            TimelineSummary timeline
    ) {
        MatchSummary restoredFromRaw = restoreFromRawJson(participant, core, timeline);
        if (restoredFromRaw != null) {
            return restoredFromRaw;
        }

        MatchSummary summary = new MatchSummary();
        summary.setMatchId(participant.getMatchId());
        summary.setQueueId(nullSafeInt(core.getQueueId()));
        summary.setGameDurationMinutes(Math.max(1, nullSafeInt(core.getGameDurationSeconds()) / 60));
        summary.setGameEndTimeStamp(nullSafeLong(core.getGameEndTimestamp()));

        boolean remake = nullSafeInt(core.getGameDurationSeconds()) > 0
                && nullSafeInt(core.getGameDurationSeconds()) < 240;
        boolean win = Boolean.TRUE.equals(participant.getIsWin());

        MatchResultType resultType = remake
                ? MatchResultType.REMAKE
                : (win ? MatchResultType.WIN : MatchResultType.LOSS);

        summary.setResultType(resultType);
        summary.setRemake(remake);
        summary.setInvalid(false);
        summary.setCountedGame(!remake);
        summary.setWin(win && !remake);
        summary.setLoss(!win && !remake);
        summary.setDisplayResult(resolveDisplayResult(resultType));

        summary.setParticipantId(nullSafeInt(participant.getParticipantId()));
        summary.setTeamId(nullSafeInt(participant.getTeamId()));
        summary.setTeamPosition(safeString(participant.getTeamPosition()));
        summary.setChampionName(safeString(participant.getChampionName()));
        summary.setKills(nullSafeInt(participant.getKills()));
        summary.setDeaths(nullSafeInt(participant.getDeaths()));
        summary.setAssists(nullSafeInt(participant.getAssists()));
        summary.setTotalCs(nullSafeInt(participant.getTotalCs()));
        summary.setGoldEarned(nullSafeInt(participant.getGoldEarned()));
        summary.setDamageToChampions(nullSafeInt(participant.getDamageToChampions()));
        summary.setVisionScore(nullSafeInt(participant.getVisionScore()));
        summary.setWardsPlaced(nullSafeInt(participant.getWardsPlaced()));
        summary.setWardsKilled(nullSafeInt(participant.getWardsKilled()));
        summary.setControlWardsPlaced(nullSafeInt(participant.getControlWardsPlaced()));
        summary.setTotalTimeSpentDead(nullSafeInt(participant.getTotalTimeSpentDead()));

        summary.setItems(Collections.emptyList());
        summary.setTeamMembers(Collections.emptyList());
        summary.setTeamChamps(Collections.emptyList());
        summary.setSpell1Id(0);
        summary.setSpell2Id(0);
        summary.setMainRuneId(0);
        summary.setSubRuneId(0);
        summary.setPerformanceScore(0);
        summary.setRiotId("");
        summary.setChampionLevel(0);
        summary.setDamageToObjectives(0);
        summary.setDamageToTurrets(0);
        summary.setTeamKills(0);
        summary.setTeamGoldEarned(0);
        summary.setTeamDamageToChampions(0);
        summary.setKillParticipation(0.0);
        summary.setDamageShare(0.0);
        summary.setDamageConversion(0.0);
        summary.setVisionPerMinute(0.0);
        summary.setGoldPerMinute(0.0);
        summary.setCsPerMinute(0.0);
        summary.setTimeAliveRatio(0.0);
        summary.setBaseDelta(0);
        summary.setPerformanceDelta(0);
        summary.setFinalDelta(0);
        summary.setPerfIndex(0.0);
        summary.setGrowthScore(0.0);
        summary.setTeamplayScore(0.0);
        summary.setEfficiencyScore(0.0);
        summary.setSurvivalScore(0.0);
        summary.setScoreTier("");
        summary.setLeaver(false);

        if (timeline != null) {
            summary.setGoldDiff15(nullSafeInt(timeline.getGoldDiff15()));
            summary.setCsDiff15(nullSafeInt(timeline.getCsDiff15()));
            summary.setXpDiff15(nullSafeInt(timeline.getXpDiff15()));
            summary.setObjectiveParticipationScore(nullSafeInt(timeline.getObjectiveParticipationScore()));
            summary.setThrowDeathPenalty(nullSafeInt(timeline.getThrowDeathPenalty()));
        }

        return summary;
    }

    private MatchSummary restoreFromRawJson(
            MatchParticipant participant,
            MatchCore core,
            TimelineSummary timeline
    ) {
        if (!StringUtils.hasText(participant.getRawJson())) {
            return null;
        }

        try {
            MatchSummary summary = objectMapper.readValue(participant.getRawJson(), MatchSummary.class);

            if (summary.getQueueId() <= 0) {
                summary.setQueueId(nullSafeInt(core.getQueueId()));
            }
            if (summary.getGameDurationMinutes() <= 0) {
                summary.setGameDurationMinutes(Math.max(1, nullSafeInt(core.getGameDurationSeconds()) / 60));
            }
            if (summary.getGameEndTimeStamp() <= 0) {
                summary.setGameEndTimeStamp(nullSafeLong(core.getGameEndTimestamp()));
            }
            if (timeline != null) {
                if (summary.getGoldDiff15() == 0) {
                    summary.setGoldDiff15(nullSafeInt(timeline.getGoldDiff15()));
                }
                if (summary.getCsDiff15() == 0) {
                    summary.setCsDiff15(nullSafeInt(timeline.getCsDiff15()));
                }
                if (summary.getXpDiff15() == 0) {
                    summary.setXpDiff15(nullSafeInt(timeline.getXpDiff15()));
                }
                if (summary.getObjectiveParticipationScore() == 0) {
                    summary.setObjectiveParticipationScore(nullSafeInt(timeline.getObjectiveParticipationScore()));
                }
                if (summary.getThrowDeathPenalty() == 0) {
                    summary.setThrowDeathPenalty(nullSafeInt(timeline.getThrowDeathPenalty()));
                }
            }

            if (summary.getItems() == null) {
                summary.setItems(Collections.emptyList());
            }
            if (summary.getTeamMembers() == null) {
                summary.setTeamMembers(Collections.emptyList());
            }
            if (summary.getTeamChamps() == null) {
                summary.setTeamChamps(Collections.emptyList());
            }

            return summary;
        } catch (Exception e) {
            return null;
        }
    }

    private void saveRankSnapshot(
            String puuid,
            String queueType,
            SummonerDTO.RankInfo rankInfo
    ) {
        if (!StringUtils.hasText(puuid) || !StringUtils.hasText(queueType)) {
            return;
        }

        RankSnapshotId id = new RankSnapshotId();
        id.setPuuid(puuid);
        id.setQueueType(queueType);

        RankSnapshot snapshot = rankSnapshotRepository.findById(id)
                .orElseGet(RankSnapshot::new);

        boolean changed = false;
        changed |= setIfDifferent(snapshot.getPuuid(), puuid, snapshot::setPuuid);
        changed |= setIfDifferent(snapshot.getQueueType(), queueType, snapshot::setQueueType);

        if (rankInfo != null && StringUtils.hasText(rankInfo.getTier())
                && !"UNRANKED".equalsIgnoreCase(rankInfo.getTier())) {
            changed |= setIfDifferent(snapshot.getTier(), rankInfo.getTier(), snapshot::setTier);
            changed |= setIfDifferent(snapshot.getRank(), rankInfo.getRank(), snapshot::setRank);
            changed |= setIfDifferent(snapshot.getLeaguePoints(), rankInfo.getLeaguePoints(), snapshot::setLeaguePoints);
        } else {
            changed |= setIfDifferent(snapshot.getTier(), null, snapshot::setTier);
            changed |= setIfDifferent(snapshot.getRank(), null, snapshot::setRank);
            changed |= setIfDifferent(snapshot.getLeaguePoints(), null, snapshot::setLeaguePoints);
        }

        if (changed || snapshot.getFetchedAt() == null) {
            snapshot.setWins(null);
            snapshot.setLosses(null);
            snapshot.setFetchedAt(OffsetDateTime.now());
            rankSnapshotRepository.save(snapshot);
        }
    }

    private void saveMatchCore(MatchSummary match) {
        MatchCore core = matchCoreRepository.findById(match.getMatchId())
                .orElseGet(MatchCore::new);

        boolean changed = false;
        changed |= setIfDifferent(core.getMatchId(), match.getMatchId(), core::setMatchId);
        changed |= setIfDifferent(core.getQueueId(), match.getQueueId(), core::setQueueId);
        changed |= setIfDifferent(core.getGameDurationSeconds(), Math.max(match.getGameDurationMinutes(), 0) * 60, core::setGameDurationSeconds);
        changed |= setIfDifferent(core.getGameEndTimestamp(), match.getGameEndTimeStamp(), core::setGameEndTimestamp);

        if (changed || core.getFetchedAt() == null) {
            core.setGameVersion(null);
            core.setFetchedAt(OffsetDateTime.now());
            matchCoreRepository.save(core);
        }
    }

    private void saveMatchParticipant(String puuid, MatchSummary match) {
        MatchParticipantId id = new MatchParticipantId();
        id.setMatchId(match.getMatchId());
        id.setPuuid(puuid);

        MatchParticipant participant = matchParticipantRepository.findById(id)
                .orElseGet(MatchParticipant::new);

        String rawJson = serializeMatchSummary(match);
        boolean changed = false;

        changed |= setIfDifferent(participant.getMatchId(), match.getMatchId(), participant::setMatchId);
        changed |= setIfDifferent(participant.getPuuid(), puuid, participant::setPuuid);
        changed |= setIfDifferent(participant.getParticipantId(), match.getParticipantId(), participant::setParticipantId);
        changed |= setIfDifferent(participant.getTeamId(), match.getTeamId(), participant::setTeamId);
        changed |= setIfDifferent(participant.getTeamPosition(), match.getTeamPosition(), participant::setTeamPosition);
        changed |= setIfDifferent(participant.getChampionName(), match.getChampionName(), participant::setChampionName);
        changed |= setIfDifferent(participant.getKills(), match.getKills(), participant::setKills);
        changed |= setIfDifferent(participant.getDeaths(), match.getDeaths(), participant::setDeaths);
        changed |= setIfDifferent(participant.getAssists(), match.getAssists(), participant::setAssists);
        changed |= setIfDifferent(participant.getTotalCs(), match.getTotalCs(), participant::setTotalCs);
        changed |= setIfDifferent(participant.getGoldEarned(), match.getGoldEarned(), participant::setGoldEarned);
        changed |= setIfDifferent(participant.getDamageToChampions(), match.getDamageToChampions(), participant::setDamageToChampions);
        changed |= setIfDifferent(participant.getVisionScore(), match.getVisionScore(), participant::setVisionScore);
        changed |= setIfDifferent(participant.getWardsPlaced(), match.getWardsPlaced(), participant::setWardsPlaced);
        changed |= setIfDifferent(participant.getWardsKilled(), match.getWardsKilled(), participant::setWardsKilled);
        changed |= setIfDifferent(participant.getControlWardsPlaced(), match.getControlWardsPlaced(), participant::setControlWardsPlaced);
        changed |= setIfDifferent(participant.getTotalTimeSpentDead(), match.getTotalTimeSpentDead(), participant::setTotalTimeSpentDead);
        changed |= setIfDifferent(participant.getIsWin(), match.isWin(), participant::setIsWin);
        changed |= setIfDifferent(participant.getRawJson(), rawJson, participant::setRawJson);

        if (changed || participant.getFetchedAt() == null) {
            participant.setFetchedAt(OffsetDateTime.now());
            matchParticipantRepository.save(participant);
        }
    }

    private void saveTimelineSummary(String puuid, MatchSummary match) {
        TimelineSummaryId id = new TimelineSummaryId(match.getMatchId(), puuid);

        TimelineSummary timeline = timelineSummaryRepository.findById(id)
                .orElseGet(TimelineSummary::new);

        boolean changed = false;
        changed |= setIfDifferent(timeline.getMatchId(), match.getMatchId(), timeline::setMatchId);
        changed |= setIfDifferent(timeline.getPuuid(), puuid, timeline::setPuuid);
        changed |= setIfDifferent(timeline.getGoldDiff15(), match.getGoldDiff15(), timeline::setGoldDiff15);
        changed |= setIfDifferent(timeline.getCsDiff15(), match.getCsDiff15(), timeline::setCsDiff15);
        changed |= setIfDifferent(timeline.getXpDiff15(), match.getXpDiff15(), timeline::setXpDiff15);
        changed |= setIfDifferent(timeline.getObjectiveParticipationScore(), match.getObjectiveParticipationScore(), timeline::setObjectiveParticipationScore);
        changed |= setIfDifferent(timeline.getThrowDeathPenalty(), match.getThrowDeathPenalty(), timeline::setThrowDeathPenalty);

        if (changed || timeline.getTimelineFetchedAt() == null) {
            timeline.setTimelineFetchedAt(OffsetDateTime.now());
            timelineSummaryRepository.save(timeline);
        }
    }

    private String serializeMatchSummary(MatchSummary match) {
        try {
            return objectMapper.writeValueAsString(match);
        } catch (Exception e) {
            return null;
        }
    }

    private List<MatchSummary> safeList(List<MatchSummary> matches) {
        return matches == null ? Collections.emptyList() : matches;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }

    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private long nullSafeLong(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveDisplayResult(MatchResultType resultType) {
        return switch (resultType) {
            case WIN -> "승리";
            case LOSS -> "패배";
            case REMAKE -> "다시하기";
            case INVALID -> "분석 제외";
        };
    }

    private <T> boolean setIfDifferent(T currentValue, T newValue, java.util.function.Consumer<T> setter) {
        if (Objects.equals(currentValue, newValue)) {
            return false;
        }
        setter.accept(newValue);
        return true;
    }
}