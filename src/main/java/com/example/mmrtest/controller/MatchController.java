package com.example.mmrtest.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
    public ResponseEntity<?> getMatchAnalysis(
            @PathVariable String matchId,
            @RequestParam String puuid,
            @RequestParam(defaultValue = "3") int bucketMinutes
    ) {
        if (!StringUtils.hasText(matchId)) {
            return ResponseEntity.badRequest().body(errorBody("matchId가 비어 있습니다."));
        }

        if (!StringUtils.hasText(puuid)) {
            return ResponseEntity.badRequest().body(errorBody("puuid가 비어 있습니다."));
        }

        int normalizedBucketMinutes = normalizeBucketMinutes(bucketMinutes);

        MatchAnalysisDetail detail = matchAnalysisService.buildMatchAnalysis(
                puuid,
                matchId,
                normalizedBucketMinutes
        );

        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(detail);
    }

    private int normalizeBucketMinutes(int bucketMinutes) {
        if (bucketMinutes == 5 || bucketMinutes == 10) {
            return bucketMinutes;
        }
        return 3;
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
}