package com.shadiwaley.server.match.application.service;

import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.match.dto.response.ProposalEligibilityResult;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import org.springframework.stereotype.Service;

@Service
public class ProposalEligibilityService {

    public ProposalEligibilityResult checkEligibility(
            CrmCase crmCase,
            UserProfile sourceProfile,
            UserProfile targetProfile,
            boolean alreadyProposed,
            boolean alreadyAccepted,
            Integer score
    ) {
        if (crmCase != null && crmCase.getStatus() == CrmCaseStatus.CLOSED) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(false)
                    .dispatchReadiness("BLOCKED")
                    .proposalBlockReason("Case is closed. Reopen the case before sending proposals.")
                    .build();
        }

        if (!isEligibleProfile(sourceProfile)) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(false)
                    .dispatchReadiness("BLOCKED")
                    .proposalBlockReason("Source profile is not eligible for matchmaking.")
                    .build();
        }

        if (!isEligibleProfile(targetProfile)) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(false)
                    .dispatchReadiness("BLOCKED")
                    .proposalBlockReason("Target profile is not live or verified.")
                    .build();
        }

        if (alreadyAccepted) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(false)
                    .dispatchReadiness("ALREADY_DISPATCHED")
                    .proposalBlockReason("Proposal already accepted.")
                    .build();
        }

        if (alreadyProposed) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(false)
                    .dispatchReadiness("ALREADY_DISPATCHED")
                    .proposalBlockReason("Proposal already sent.")
                    .build();
        }

        if (score != null && score >= 70) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(true)
                    .dispatchReadiness("READY_TO_SEND")
                    .proposalBlockReason(null)
                    .build();
        }

        if (score != null && score >= 50) {
            return ProposalEligibilityResult.builder()
                    .canSendProposal(true)
                    .dispatchReadiness("NEEDS_REVIEW")
                    .proposalBlockReason(null)
                    .build();
        }

        return ProposalEligibilityResult.builder()
                .canSendProposal(true)
                .dispatchReadiness("LOW_CONFIDENCE")
                .proposalBlockReason(null)
                .build();
    }

    private boolean isEligibleProfile(UserProfile profile) {
        return profile != null
                && (
                profile.getProfileStatus() == ProfileStatus.LIVE
                        || profile.getProfileStatus() == ProfileStatus.VERIFIED
        );
    }
}