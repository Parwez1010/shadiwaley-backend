package com.shadiwaley.server.proposal.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ProposalMiniProfileResponse {

    private UUID profileId;
    private String candidateName;
    private String side;
    private Integer age;
    private String district;
    private String caste;
    private String maslak;
}