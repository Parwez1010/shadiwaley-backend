package com.shadiwaley.server.customer.proposal.dto.response;

import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class CustomerProposalItemResponse {

    private UUID requestId;

    private UUID otherUserId;
    private UUID otherProfileId;

    private String candidateName;
    private String parentName;
    private String phone;
    private String side;

    private Short age;
    private String district;
    private String state;
    private String caste;
    private String maslak;
    private String education;
    private String professionTitle;

    private RishtaRequestStatus status;

    private String senderNote;

    private boolean chatEnabled;
    private UUID chatRoomId;

    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    private boolean canAccept;
    private boolean canReject;
    private boolean canCancel;
}