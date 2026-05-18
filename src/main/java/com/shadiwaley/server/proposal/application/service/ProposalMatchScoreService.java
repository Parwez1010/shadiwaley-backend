package com.shadiwaley.server.proposal.application.service;

import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import lombok.Getter;
import org.springframework.stereotype.Service;

@Service
public class ProposalMatchScoreService {

    public MatchScoreResult calculate(UserProfile source, UserProfile target) {
        int rawScore = 0;
        int maxPossibleScore = 10;

        if (target.getCompletionPct() != null
                && target.getCompletionPct() >= 80) {
            rawScore += 5;
        }

        int normalizedScore =
                (int) Math.round((rawScore * 100.0) / maxPossibleScore);

        return new MatchScoreResult(normalizedScore);
    }

    @Getter
    public static class MatchScoreResult {
        private final int score;

        public MatchScoreResult(int score) {
            this.score = score;
        }
    }
}