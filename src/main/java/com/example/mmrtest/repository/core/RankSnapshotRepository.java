package com.example.mmrtest.repository.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mmrtest.entity.core.RankSnapshot;
import com.example.mmrtest.entity.core.RankSnapshotId;

public interface RankSnapshotRepository extends JpaRepository<RankSnapshot, RankSnapshotId> {

    List<RankSnapshot> findByPuuid(String puuid);

    Optional<RankSnapshot> findTopByPuuidAndQueueTypeOrderByFetchedAtDesc(String puuid, String queueType);
}