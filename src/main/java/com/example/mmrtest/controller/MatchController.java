package com.example.mmrtest.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mmrtest.dto.MatchAnalysisDetail;
import com.example.mmrtest.service.MatchAnalysisService;

@RestController
public class MatchController {

    private final MatchAnalysisService matchAnalysisService;

    public MatchController(MatchAnalysisService matchAnalysisService) {
        this.matchAnalysisService = matchAnalysisService;
    }

    @GetMapping("/api/matches/{matchId}/analysis")
    public Object getMatchAnalysis(
            @PathVariable String matchId,
            @RequestParam String puuid
    ) {
        try {
            if (matchId == null || matchId.isBlank()) {
                return Map.of("error", "matchId가 비어 있습니다.");
            }

            if (puuid == null || puuid.isBlank()) {
                return Map.of("error", "puuid가 비어 있습니다.");
            }

            MatchAnalysisDetail detail = matchAnalysisService.buildMatchAnalysis(puuid, matchId);
            if (detail == null) {
                return Map.of("error", "상세 분석 데이터를 찾을 수 없습니다.");
            }

            return detail;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("error", "상세 분석 조회 실패: " + e.getMessage());
        }
    }
}