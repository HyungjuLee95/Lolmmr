package com.example.mmrtest.controller;

import com.example.mmrtest.dto.SummonerDTO;
import com.example.mmrtest.service.SummonerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class SummonerController {

    private final SummonerService summonerService;

    public SummonerController(SummonerService summonerService) {
        this.summonerService = summonerService;
    }

    @Value("${dev.allowlist.enabled:false}")
    private boolean allowlistEnabled;

    @Value("${dev.allowlist.riotIds:}")
    private List<String> allowedRiotIds;

    @GetMapping("/mmr")
    public Map<String, Object> getMmrAnalysis(@RequestParam("name") String name) {
        try {
            // 1. 디코딩 및 이름/태그 분리
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

            // [추가] 개발 모드 Allowlist 체크
            if (allowlistEnabled) {
                if (!allowedRiotIds.contains(fullRiotId)) {
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("error", "개발 모드입니다. 허용된 계정만 조회 가능합니다.");
                    // 차후 적절한 status 코드로 변경 권장 (ex. ResponseEntity)
                    return errorResult;
                }
            }

            // 2. 서비스의 통합 분석 메서드 호출
            Map<String, Object> analysisResult = summonerService.getMmrAnalysis(gameName, tagLine);
            SummonerDTO summoner = (SummonerDTO) analysisResult.get("summoner");

            // 3. 차후 확장 포맷 ("queues.solo", "queues.flex")으로 래핑
            Map<String, Object> finalResponse = new HashMap<>();

            // 기존과 호환성을 위해 상단에 포함 (추후 삭제 고려)
            finalResponse.put("summoner", summoner);

            Map<String, Object> queues = new HashMap<>();

            Map<String, Object> soloData = new HashMap<>();
            soloData.put("matchDetails", analysisResult.get("soloMatchDetails"));
            soloData.put("lpChange", analysisResult.get("soloLpChange"));
            soloData.put("standardMmr", summonerService.convertTierToMmr(summoner.getSoloRank().getTier(),
                    summoner.getSoloRank().getRank()));
            soloData.put("scoreResult", analysisResult.get("soloScoreResult"));
            queues.put("solo", soloData);

            Map<String, Object> flexData = new HashMap<>();
            flexData.put("matchDetails", analysisResult.get("flexMatchDetails"));
            flexData.put("lpChange", analysisResult.get("flexLpChange"));
            flexData.put("standardMmr", summonerService.convertTierToMmr(summoner.getFlexRank().getTier(),
                    summoner.getFlexRank().getRank()));
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
}