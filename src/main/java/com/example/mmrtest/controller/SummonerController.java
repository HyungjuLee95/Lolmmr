package com.example.mmrtest.controller;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.service.SummonerService;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class SummonerController {

    private static final Logger log = LoggerFactory.getLogger(SummonerController.class);
    private static final int DISPLAY_MATCH_COUNT = 20;
    private static final int SCORE_SAMPLE_COUNT = 20;

    private final SummonerService summonerService;

    public SummonerController(SummonerService summonerService) {
        this.summonerService = summonerService;
    }

    @Value("${dev.allowlist.enabled:false}")
    private String allowlistEnabledRaw;

    @Value("${dev.allowlist.riotIds:}")
    private List<String> allowedRiotIds;

    @GetMapping("/mmr")
    public Map<String, Object> getMmrAnalysis(
            @RequestParam("name") String name,
            @RequestParam(value = "queue", required = false, defaultValue = "solo") String queue,
            @RequestParam(value = "forceRefresh", required = false, defaultValue = "false") boolean forceRefresh
    ) {
        try {
            String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8).trim();

            if (decodedName.isBlank()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "소환사 이름은 비어 있을 수 없습니다.");
                return errorResult;
            }

            String gameName;
            String tagLine;

            if (decodedName.contains("#")) {
                String[] parts = decodedName.split("#", 2);
                gameName = parts[0].trim();
                tagLine = parts[1].trim();
            } else {
                gameName = decodedName;
                tagLine = "KR1";
            }

            if (gameName.isBlank() || tagLine.isBlank()) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "올바른 Riot ID 형식이 아닙니다. (예: gameName#KR1)");
                return errorResult;
            }

            String fullRiotId = gameName + "#" + tagLine;

            if (isAllowlistEnabled() && !getNormalizedAllowlist().contains(normalizeRiotId(fullRiotId))) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "개발 모드입니다. 허용된 계정만 조회 가능합니다.");
                return errorResult;
            }

            Map<String, Object> analysisResult = summonerService.getMmrAnalysis(gameName, tagLine, queue, forceRefresh);
            SummonerDTO summoner = (SummonerDTO) analysisResult.get("summoner");

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("summoner", summoner);

            Map<String, Object> queues = new HashMap<>();

            Map<String, Object> counts = (Map<String, Object>) analysisResult.get("counts");

            Map<String, Object> soloData = new HashMap<>();
            soloData.put("matchDetails", analysisResult.get("soloMatchDetails"));
            soloData.put("lpChange", analysisResult.get("soloLpChange"));
            String soloTier = summoner.getSoloRank() != null ? summoner.getSoloRank().getTier() : "UNRANKED";
            String soloRank = summoner.getSoloRank() != null ? summoner.getSoloRank().getRank() : "";
            soloData.put("standardMmr", summonerService.convertTierToMmr(soloTier, soloRank));
            soloData.put("scoreResult", analysisResult.get("soloScoreResult"));
            soloData.put("summary", analysisResult.get("soloSummary"));
            soloData.put("counts", counts.get("solo"));
            queues.put("solo", soloData);

            Map<String, Object> flexData = new HashMap<>();
            flexData.put("matchDetails", analysisResult.get("flexMatchDetails"));
            flexData.put("lpChange", analysisResult.get("flexLpChange"));
            String flexTier = summoner.getFlexRank() != null ? summoner.getFlexRank().getTier() : "UNRANKED";
            String flexRank = summoner.getFlexRank() != null ? summoner.getFlexRank().getRank() : "";
            flexData.put("standardMmr", summonerService.convertTierToMmr(flexTier, flexRank));
            flexData.put("scoreResult", analysisResult.get("flexScoreResult"));
            flexData.put("summary", analysisResult.get("flexSummary"));
            flexData.put("counts", counts.get("flex"));
            queues.put("flex", flexData);

            finalResponse.put("queues", queues);
            finalResponse.put("activeGame", analysisResult.get("activeGame"));
            finalResponse.put("championMasteries", analysisResult.get("championMasteries"));

            Map<String, Object> meta = new HashMap<>();
            meta.put("requestedQueue", analysisResult.get("requestedQueue"));
            meta.put("resolvedQueue", analysisResult.get("resolvedQueue"));
            meta.put("counts", counts);
            meta.put("analysisMode", "light");
            meta.put("displayMatchCount", DISPLAY_MATCH_COUNT);
            meta.put("scoreSampleCount", SCORE_SAMPLE_COUNT);
            meta.put("availableQueues", List.of("solo", "flex"));
            meta.put("riotId", fullRiotId);
            finalResponse.put("meta", meta);

            return finalResponse;

        } catch (Exception e) {
            log.error("Failed to analyze MMR for name='{}', queue='{}'", name, queue, e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", "조회 실패: " + e.getMessage());
            return errorResult;
        }
    }

    private boolean isAllowlistEnabled() {
        if (allowlistEnabledRaw == null) {
            return false;
        }

        String normalized = allowlistEnabledRaw.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }

        log.warn("Invalid boolean value for dev.allowlist.enabled='{}'. Fallback to false.", allowlistEnabledRaw);
        return false;
    }

    private Set<String> getNormalizedAllowlist() {
        if (allowedRiotIds == null) {
            return Collections.emptySet();
        }

        return allowedRiotIds.stream()
                .map(this::normalizeRiotId)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String normalizeRiotId(String riotId) {
        if (riotId == null) {
            return "";
        }

        return riotId.trim().toLowerCase(Locale.ROOT);
    }
}
