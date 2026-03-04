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

    public ScoreResult calculateScore(List<MatchSummary> matches, int baseScore) {
        if (matches == null || matches.isEmpty()) {
            return new ScoreResult(baseScore, new ArrayList<>());
        }

        // 시간순 정렬 (오래된 순 -> 최신 순)
        List<MatchSummary> sortedMatches = matches.stream()
                .sorted(Comparator.comparingLong(MatchSummary::getGameEndTimeStamp))
                .collect(Collectors.toList());

        int currentScore = baseScore;
        List<Integer> scoreHistory = new ArrayList<>();

        for (MatchSummary match : sortedMatches) {
            if (match.isWin()) {
                currentScore += 20;
            } else {
                currentScore -= 20;
            }
            scoreHistory.add(currentScore);
        }

        return new ScoreResult(currentScore, scoreHistory);
    }
}
