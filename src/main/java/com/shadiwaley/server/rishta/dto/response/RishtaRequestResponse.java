package com.shadiwaley.server.rishta.dto.response;

import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class RishtaRequestResponse {

    private UUID requestId;

    private UUID profileId;

    private String displayId;

    private String candidateFirstName;

    private Short candidateAge;

    private String district;

    private String state;

    private String professionTitle;

    private String education;

    private boolean hasApprovedPhoto;

    private RishtaRequestStatus status;

    private String senderNote;

    private boolean chatEnabled;

    private Instant createdAt;
}