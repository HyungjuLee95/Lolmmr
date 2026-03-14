package com.example.mmrtest.repository.core;

import com.example.mmrtest.entity.core.MatchParticipant;
import com.example.mmrtest.entity.core.MatchParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, MatchParticipantId> {
    List<MatchParticipant> findByPuuidOrderByFetchedAtDesc(String puuid);
    List<MatchParticipant> findByMatchId(String matchId);
}
