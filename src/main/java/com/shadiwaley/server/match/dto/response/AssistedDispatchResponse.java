package com.shadiwaley.server.match.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class AssistedDispatchResponse {

    private UUID crmCaseId;
    private UUID sourceProfileId;
    private String sourceCandidateName;

    private List<AssistedDispatchSuggestionResponse> suggestions;
}