package com.shadiwaley.server.match.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.match.application.service.AssistedDispatchService;
import com.shadiwaley.server.match.application.service.MatchSuggestionService;
import com.shadiwaley.server.match.dto.response.AssistedDispatchResponse;
import com.shadiwaley.server.match.dto.response.CompatibilityResponse;
import com.shadiwaley.server.match.dto.response.MatchSuggestionPageResponse;
import com.shadiwaley.server.match.dto.response.MatchSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/matches")
@RequiredArgsConstructor
public class MatchSuggestionController {

    private final MatchSuggestionService matchSuggestionService;
    private final AssistedDispatchService assistedDispatchService;

    @GetMapping("/suggestions/{profileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<MatchSuggestionPageResponse> suggestions(
            @PathVariable UUID profileId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String caste,
            @RequestParam(required = false) String maslak,
            @RequestParam(required = false) Short minAge,
            @RequestParam(required = false) Short maxAge,
            @RequestParam(required = false) String education,
            @RequestParam(required = false) String professionType,
            @RequestParam(required = false) String familyType,
            @RequestParam(required = false) Integer minIncome,
            @RequestParam(required = false) Integer maxIncome,
            @RequestParam(required = false) Boolean verifiedOnly,
            @RequestParam(required = false) Boolean hasPhotoOnly,
            @RequestParam(required = false) Boolean notPreviouslyProposed,
            @RequestParam(required = false) String readiness,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(required = false) Integer maxScore,
            @RequestParam(defaultValue = "BEST_MATCH") String sort
    ) {
        return ResponseFactory.success(
                "Match suggestions fetched successfully",
                matchSuggestionService.getSuggestions(
                        profileId,
                        page,
                        size,
                        search,
                        district,
                        state,
                        caste,
                        maslak,
                        minAge,
                        maxAge,
                        education,
                        professionType,
                        familyType,
                        minIncome,
                        maxIncome,
                        verifiedOnly,
                        hasPhotoOnly,
                        notPreviouslyProposed,
                        readiness,
                        minScore,
                        maxScore,
                        sort
                )
        );
    }

    @GetMapping("/compatibility")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<CompatibilityResponse> compatibility(
            @RequestParam UUID profileAId,
            @RequestParam UUID profileBId
    ) {

        return ResponseFactory.success(
                "Compatibility calculated successfully",
                matchSuggestionService.compatibility(profileAId, profileBId)
        );
    }

    @GetMapping("/assisted-dispatch/{crmCaseId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<AssistedDispatchResponse> assistedDispatch(
            @PathVariable UUID crmCaseId
    ) {
        return ResponseFactory.success(
                "Assisted dispatch suggestions fetched successfully",
                assistedDispatchService.getAssistedDispatch(crmCaseId)
        );
    }

    @GetMapping("/suggestions/{sourceProfileId}/candidate/{targetProfileId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<MatchSuggestionResponse> candidatePreview(
            @PathVariable UUID sourceProfileId,
            @PathVariable UUID targetProfileId
    ) {
        return ResponseFactory.success(
                "Candidate preview fetched successfully",
                matchSuggestionService.getCandidatePreview(sourceProfileId, targetProfileId)
        );
    }

}