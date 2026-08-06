package com.shadiwaley.server.matchmaking.application.service;

import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.profile.dto.response.MatchBreakdownResponse;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class MatchScoreService {

    public MatchBreakdownResponse calculate(
            UserProfile viewerProfile,
            ParentProfile viewerParent,
            UserPreferences viewerPreferences,
            UserProfile candidateProfile,
            ParentProfile candidateParent
    ) {
        int score = 0;

        boolean maslakMatched = matches(
                viewerPreferences.getPreferredMaslak(),
                candidateParent.getMaslak()
        );

        boolean districtMatched = matches(
                viewerPreferences.getPreferredDistrict(),
                candidateParent.getDistrict()
        );

        boolean ageMatched = isAgeMatched(
                candidateProfile.getCandidateAge(),
                viewerPreferences.getMinAge(),
                viewerPreferences.getMaxAge()
        );

        boolean educationMatched = matches(
                viewerPreferences.getPreferredEducation(),
                candidateProfile.getEducation()
        );

        boolean familyTypeMatched = matches(
                viewerPreferences.getPreferredFamilyType(),
                candidateProfile.getFamilyType()
        );

        boolean dietMatched = viewerProfile.getDiet() != null
                && viewerProfile.getDiet() == candidateProfile.getDiet();

        boolean valuesMatched = viewerProfile.getFamilyValues() != null
                && viewerProfile.getFamilyValues() == candidateProfile.getFamilyValues();

        boolean sectMatched = matches(viewerProfile.getSect(), candidateProfile.getSect());

        if (maslakMatched) score += 25;
        if (districtMatched) score += 15;
        if (ageMatched) score += 15;
        if (educationMatched) score += 12;
        if (familyTypeMatched) score += 8;
        if (sectMatched) score += 10;
        if (dietMatched) score += 8;
        if (valuesMatched) score += 7;

        if (candidateProfile.getQuranLevel() != null && !candidateProfile.getQuranLevel().isBlank()) {
            score += 3;
        }
        if (candidateProfile.getNamaazRegularity() != null && !candidateProfile.getNamaazRegularity().isBlank()) {
            score += 2;
        }

        return MatchBreakdownResponse.builder()
                .totalScore(Math.min(score, 100))
                .maslakMatched(maslakMatched)
                .districtMatched(districtMatched)
                .ageMatched(ageMatched)
                .educationMatched(educationMatched)
                .familyTypeMatched(familyTypeMatched)
                .build();
    }

    private boolean matches(String preferred, String actual) {
        if (preferred == null || preferred.isBlank()) {
            return false;
        }

        if (actual == null || actual.isBlank()) {
            return false;
        }

        return preferred.trim().equalsIgnoreCase(actual.trim());
    }

    private boolean isAgeMatched(Short candidateAge, Short minAge, Short maxAge) {
        if (candidateAge == null) {
            return false;
        }

        if (minAge != null && candidateAge < minAge) {
            return false;
        }

        if (maxAge != null && candidateAge > maxAge) {
            return false;
        }

        return minAge != null || maxAge != null;
    }
}