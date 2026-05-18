package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchFilterMetadataResponse {

    private List<String> districts;
    private List<String> states;
    private List<String> castes;
    private List<String> maslak;
    private List<String> education;
    private List<String> professionTypes;
    private List<String> familyTypes;
    private List<ScoreRangeResponse> scoreRanges;
}