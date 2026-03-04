package com.example.mmrtest.controller;

import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.service.SummonerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class SummonerController {

    private static final Logger log = LoggerFactory.getLogger(SummonerController.class);

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
            @RequestParam(value = "queue", required = false, defaultValue = "solo") String queue
    ) {
        try {
            String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8);
            String gameName;
            String tagLine;

            if (decodedName.contains("#")) {
                String[] parts = decodedName.split("#");
                gameName = parts[0];
                tagLine = parts[1];
            } else {
                gameName = decodedName;
                tagLine = "KR1";
            }

            String fullRiotId = gameName + "#" + tagLine;

            if (isAllowlistEnabled() && !allowedRiotIds.contains(fullRiotId)) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", "개발 모드입니다. 허용된 계정만 조회 가능합니다.");
                return errorResult;
            }

            Map<String, Object> analysisResult = summonerService.getMmrAnalysis(gameName, tagLine, queue);
            SummonerDTO summoner = (SummonerDTO) analysisResult.get("summoner");

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("summoner", summoner);

            Map<String, Object> queues = new HashMap<>();

            Map<String, Object> soloData = new HashMap<>();
            soloData.put("matchDetails", analysisResult.get("soloMatchDetails"));
            soloData.put("lpChange", analysisResult.get("soloLpChange"));
            String soloTier = summoner.getSoloRank() != null ? summoner.getSoloRank().getTier() : "UNRANKED";
            String soloRank = summoner.getSoloRank() != null ? summoner.getSoloRank().getRank() : "";
            soloData.put("standardMmr", summonerService.convertTierToMmr(soloTier, soloRank));
            soloData.put("scoreResult", analysisResult.get("soloScoreResult"));
            queues.put("solo", soloData);

            Map<String, Object> flexData = new HashMap<>();
            flexData.put("matchDetails", analysisResult.get("flexMatchDetails"));
            flexData.put("lpChange", analysisResult.get("flexLpChange"));
            String flexTier = summoner.getFlexRank() != null ? summoner.getFlexRank().getTier() : "UNRANKED";
            String flexRank = summoner.getFlexRank() != null ? summoner.getFlexRank().getRank() : "";
            flexData.put("standardMmr", summonerService.convertTierToMmr(flexTier, flexRank));
            flexData.put("scoreResult", analysisResult.get("flexScoreResult"));
            queues.put("flex", flexData);

            finalResponse.put("queues", queues);

            return finalResponse;

        } catch (Exception e) {
            e.printStackTrace();
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

}
