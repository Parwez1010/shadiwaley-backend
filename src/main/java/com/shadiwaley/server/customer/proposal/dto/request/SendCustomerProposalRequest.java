package com.shadiwaley.server.customer.proposal.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SendCustomerProposalRequest {

    @NotNull
    private UUID targetProfileId;

    private String note;
}