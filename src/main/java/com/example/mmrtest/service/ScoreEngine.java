package com.example.mmrtest.service;

import com.example.mmrtest.dto.MatchSummary;
import com.example.mmrtest.dto.ScoreResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScoreEngine {

    private static final int WIN_DELTA = 20;
    private static final int LOSS_DELTA = -20;

    
    public ScoreResult calculateScore(List<MatchSummary> matches, int baseScore) {
        if (matches == null || matches.isEmpty()) {
            return new ScoreResult(baseScore, new ArrayList<>());
        }

        List<MatchSummary> sortedMatches = matches.stream()
                .sorted(Comparator.comparingLong(MatchSummary::getGameEndTimeStamp))
                .collect(Collectors.toList());

        int currentScore = baseScore;
        List<Integer> scoreHistory = new ArrayList<>();

        for (MatchSummary match : sortedMatches) {
            if (match == null) {
                continue;
            }

            if (match.isCountedGame()) {
                if (match.isWin()) {
                    currentScore += WIN_DELTA;
                } else {
                    currentScore += LOSS_DELTA;
                }
            }

            // remake / invalid는 점수를 바꾸지 않지만,
            // 이력 길이는 유지해두는 편이 나중에 차트 붙일 때 덜 꼬인다.
            scoreHistory.add(currentScore);
        }

        return new ScoreResult(currentScore, scoreHistory);
    }
}