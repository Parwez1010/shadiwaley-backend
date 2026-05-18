package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchSuggestionPageResponse {

    private MatchSourceProfileResponse sourceProfile;

    private List<MatchSuggestionResponse> matches;

    private MatchFilterMetadataResponse filters;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;
}