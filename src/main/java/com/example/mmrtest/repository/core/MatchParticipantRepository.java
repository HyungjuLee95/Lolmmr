package com.example.mmrtest.repository.core;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mmrtest.entity.core.MatchParticipant;
import com.example.mmrtest.entity.core.MatchParticipantId;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, MatchParticipantId> {
    List<MatchParticipant> findByPuuidOrderByFetchedAtDesc(String puuid);
    List<MatchParticipant> findByMatchId(String matchId);
}
