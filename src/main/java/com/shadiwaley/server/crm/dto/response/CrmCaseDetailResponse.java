package com.shadiwaley.server.crm.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CrmCaseDetailResponse {
    private CrmCaseResponse crmCase;
    private List<CrmNoteResponse> notes;
    private List<CrmFollowUpResponse> followUps;
}