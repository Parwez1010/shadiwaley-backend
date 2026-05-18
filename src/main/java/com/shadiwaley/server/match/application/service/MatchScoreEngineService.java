package com.shadiwaley.server.match.application.service;

import com.shadiwaley.server.match.domain.MatchReasonType;
import com.shadiwaley.server.match.dto.response.CompatibilityResponse;
import com.shadiwaley.server.match.dto.response.MatchReasonResponse;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchScoreEngineService {

    public CompatibilityResponse calculate(
            UserProfile source,
            UserProfile target,
            ParentProfile sourceParent,
            ParentProfile targetParent,
            UserPreferences preferences
    ) {
        int score = 0;
        List<MatchReasonResponse> reasons = new ArrayList<>();

        if (target.getCandidateAge() != null
                && preferences != null
                && preferences.getMinAge() != null
                && preferences.getMaxAge() != null
                && target.getCandidateAge() >= preferences.getMinAge()
                && target.getCandidateAge() <= preferences.getMaxAge()) {
            score += 15;
            reasons.add(reason(MatchReasonType.AGE_COMPATIBLE, "Age matches preference", 15));
        }

        if (sourceParent != null && targetParent != null
                && equalsIgnoreCase(sourceParent.getDistrict(), targetParent.getDistrict())) {
            score += 10;
            reasons.add(reason(MatchReasonType.SAME_DISTRICT, "Same district", 10));
        }

        if (sourceParent != null && targetParent != null
                && equalsIgnoreCase(sourceParent.getState(), targetParent.getState())) {
            score += 5;
            reasons.add(reason(MatchReasonType.SAME_STATE, "Same state", 5));
        }

        if (sourceParent != null && targetParent != null
                && equalsIgnoreCase(sourceParent.getCaste(), targetParent.getCaste())) {
            score += 10;
            reasons.add(reason(MatchReasonType.SAME_CASTE, "Same caste", 10));
        }

        if (sourceParent != null && targetParent != null
                && equalsIgnoreCase(sourceParent.getMaslak(), targetParent.getMaslak())) {
            score += 10;
            reasons.add(reason(MatchReasonType.SAME_MASLAK, "Same maslak", 10));
        }

        if (preferences != null
                && equalsIgnoreCase(preferences.getPreferredEducation(), target.getEducation())) {
            score += 10;
            reasons.add(reason(MatchReasonType.SAME_EDUCATION, "Education preference matches", 10));
        }

        if (preferences != null
                && equalsIgnoreCase(preferences.getPreferredFamilyType(), target.getFamilyType())) {
            score += 5;
            reasons.add(reason(MatchReasonType.FAMILY_MATCH, "Family type preference matches", 5));
        }

        if (target.getCompletionPct() != null && target.getCompletionPct() >= 80) {
            score += 5;
            reasons.add(reason(MatchReasonType.LIFESTYLE_MATCH, "Profile completion is strong", 5));
        }

        String recommendation = score >= 80
                ? "Highly Recommended"
                : score >= 60
                ? "Recommended"
                : score >= 40
                ? "Average Match"
                : "Low Compatibility";

        return CompatibilityResponse.builder()
                .profileAId(source.getId())
                .profileBId(target.getId())
                .totalScore(score)
                .recommendation(recommendation)
                .reasons(reasons)
                .build();
    }

    private MatchReasonResponse reason(MatchReasonType type, String message, Integer score) {
        return MatchReasonResponse.builder()
                .type(type)
                .message(message)
                .scoreContribution(score)
                .build();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }
}