package com.shadiwaley.server.proposal.dto.request;

import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SendProposalRequest {

    @NotNull
    private UUID fromProfileId;

    @NotNull
    private UUID toProfileId;

    private UUID crmCaseId;

    @Size(max = 2000)
    private String note;

    private ProposalSourceType sourceType;

    @NotNull
    private ProposalDispatchChannel dispatchChannel;

    private boolean shareProfilePhoto;
}