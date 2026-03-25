package com.example.mmrtest.repository.core;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.mmrtest.entity.core.AnalysisSnapshot;
import com.example.mmrtest.entity.core.AnalysisSnapshotId;

public interface AnalysisSnapshotRepository extends JpaRepository<AnalysisSnapshot, AnalysisSnapshotId> {
    Optional<AnalysisSnapshot> findByMatchIdAndPuuidAndBucketMinutes(
            String matchId,
            String puuid,
            Integer bucketMinutes
    );
}