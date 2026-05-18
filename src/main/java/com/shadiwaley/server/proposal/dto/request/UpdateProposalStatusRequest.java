package com.shadiwaley.server.proposal.dto.request;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateProposalStatusRequest {

    @NotNull
    private ProposalStatus status;

    @Size(max = 2000)
    private String note;
}