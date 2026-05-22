package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AutopilotDispatchSuggestionsResponse {

    private AutopilotSuggestionSourceProfileResponse sourceProfile;

    private List<AutopilotSuggestionItemResponse> suggestions;
}