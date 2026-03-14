package com.example.mmrtest.repository.core;

import com.example.mmrtest.entity.core.TimelineSummary;
import com.example.mmrtest.entity.core.TimelineSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TimelineSummaryRepository extends JpaRepository<TimelineSummary, TimelineSummaryId> {
}
