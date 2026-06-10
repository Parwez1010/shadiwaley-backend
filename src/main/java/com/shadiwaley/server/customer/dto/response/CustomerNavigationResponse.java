package com.shadiwaley.server.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CustomerNavigationResponse {

    private String side;
    private String profileStatus;
    private boolean canBrowse;
    private boolean canChat;
    private boolean canSendProposal;
    private boolean shouldCompleteProfile;
    private boolean shouldShowUnderReview;
    private boolean shouldShowRejectedState;
    private List<String> tabs;
}