package com.shadiwaley.server.profile.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchBreakdownResponse {
    private int totalScore;
    private boolean maslakMatched;
    private boolean districtMatched;
    private boolean ageMatched;
    private boolean educationMatched;
    private boolean familyTypeMatched;
}