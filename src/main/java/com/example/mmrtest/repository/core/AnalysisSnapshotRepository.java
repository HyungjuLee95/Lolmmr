package com.example.mmrtest.repository.core;

import com.example.mmrtest.entity.core.AnalysisSnapshot;
import com.example.mmrtest.entity.core.AnalysisSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisSnapshotRepository extends JpaRepository<AnalysisSnapshot, AnalysisSnapshotId> {
}
