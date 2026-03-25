package com.example.mmrtest.controller;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mmrtest.dto.MatchAnalysisDetail;
import com.example.mmrtest.service.CoreSnapshotService;
import com.example.mmrtest.service.MatchAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class MatchController {

    private static final Logger log = LoggerFactory.getLogger(MatchController.class);

    private static final Duration ANALYSIS_CACHE_TTL = Duration.ofHours(12);
    private static final String ANALYSIS_CACHE_PREFIX = "match-analysis:v1:";

    private final MatchAnalysisService matchAnalysisService;
    private final CoreSnapshotService coreSnapshotService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, Object> analysisLocks = new ConcurrentHashMap<>();

    public MatchController(
            MatchAnalysisService matchAnalysisService,
            CoreSnapshotService coreSnapshotService,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.matchAnalysisService = matchAnalysisService;
        this.coreSnapshotService = coreSnapshotService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
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
        String cacheKey = buildAnalysisCacheKey(matchId, puuid, normalizedBucketMinutes);

        MatchAnalysisDetail cached = readAnalysisCache(cacheKey);
        if (cached != null) {
            log.info("Redis match-analysis cache HIT. key={}", cacheKey);
            return ResponseEntity.ok(cached);
        }

        Object lock = analysisLocks.computeIfAbsent(cacheKey, k -> new Object());

        try {
            synchronized (lock) {
                MatchAnalysisDetail cachedAfterLock = readAnalysisCache(cacheKey);
                if (cachedAfterLock != null) {
                    log.info("Redis match-analysis cache HIT(after-lock). key={}", cacheKey);
                    return ResponseEntity.ok(cachedAfterLock);
                }

                MatchAnalysisDetail dbSnapshot = coreSnapshotService.findAnalysisSnapshot(
                        matchId,
                        puuid,
                        normalizedBucketMinutes,
                        MatchAnalysisDetail.class
                );

                if (dbSnapshot != null) {
                    log.info("DB analysis_snapshot HIT. matchId={}, puuid={}, bucket={}", matchId, puuid, normalizedBucketMinutes);
                    writeAnalysisCache(cacheKey, dbSnapshot);
                    return ResponseEntity.ok(dbSnapshot);
                }

                log.info("DB analysis_snapshot MISS. matchId={}, puuid={}, bucket={}", matchId, puuid, normalizedBucketMinutes);

                MatchAnalysisDetail detail = matchAnalysisService.buildMatchAnalysis(
                        puuid,
                        matchId,
                        normalizedBucketMinutes
                );

                if (detail == null) {
                    return ResponseEntity.notFound().build();
                }

                writeAnalysisCache(cacheKey, detail);
                saveAnalysisSnapshot(matchId, puuid, normalizedBucketMinutes, detail);

                return ResponseEntity.ok(detail);
            }
        } finally {
            analysisLocks.remove(cacheKey, lock);
        }
    }

    private void saveAnalysisSnapshot(
            String matchId,
            String puuid,
            int bucketMinutes,
            MatchAnalysisDetail detail
    ) {
        try {
            String payload = objectMapper.writeValueAsString(detail);
            coreSnapshotService.saveAnalysisSnapshot(
                    matchId,
                    puuid,
                    bucketMinutes,
                    payload,
                    OffsetDateTime.now().plus(ANALYSIS_CACHE_TTL)
            );
            log.info("DB analysis_snapshot UPSERT. matchId={}, puuid={}, bucket={}", matchId, puuid, bucketMinutes);
        } catch (Exception e) {
            log.warn(
                    "DB analysis_snapshot write failed. matchId={}, puuid={}, bucket={}, cause={}",
                    matchId,
                    puuid,
                    bucketMinutes,
                    e.getMessage()
            );
        }
    }

    private int normalizeBucketMinutes(int bucketMinutes) {
        if (bucketMinutes == 5 || bucketMinutes == 10) {
            return bucketMinutes;
        }
        return 3;
    }

    private String buildAnalysisCacheKey(String matchId, String puuid, int bucketMinutes) {
        return ANALYSIS_CACHE_PREFIX + matchId + ":" + puuid + ":" + bucketMinutes;
    }

    private MatchAnalysisDetail readAnalysisCache(String cacheKey) {
        try {
            String cachedJson = redisTemplate.opsForValue().get(cacheKey);
            if (!StringUtils.hasText(cachedJson)) {
                return null;
            }
            return objectMapper.readValue(cachedJson, MatchAnalysisDetail.class);
        } catch (Exception e) {
            log.warn("Redis match-analysis cache read failed. key={}, cause={}", cacheKey, e.getMessage());
            return null;
        }
    }

    private void writeAnalysisCache(String cacheKey, MatchAnalysisDetail detail) {
        try {
            String payload = objectMapper.writeValueAsString(detail);
            redisTemplate.opsForValue().set(cacheKey, payload, ANALYSIS_CACHE_TTL);
            log.info("Redis match-analysis cache SET. key={}, ttlSeconds={}", cacheKey, ANALYSIS_CACHE_TTL.getSeconds());
        } catch (Exception e) {
            log.warn("Redis match-analysis cache write failed. key={}, cause={}", cacheKey, e.getMessage());
        }
    }

    private Map<String, Object> errorBody(String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", message);
        return body;
    }
}