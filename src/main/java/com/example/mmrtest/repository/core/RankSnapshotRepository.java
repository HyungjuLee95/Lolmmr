package com.example.mmrtest.repository.core;

import com.example.mmrtest.entity.core.RankSnapshot;
import com.example.mmrtest.entity.core.RankSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RankSnapshotRepository extends JpaRepository<RankSnapshot, RankSnapshotId> {
    List<RankSnapshot> findByPuuid(String puuid);
}
