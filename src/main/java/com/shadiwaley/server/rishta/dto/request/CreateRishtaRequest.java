package com.shadiwaley.server.rishta.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateRishtaRequest {

    @NotNull(message = "Receiver profile id is required")
    private UUID receiverProfileId;

    @Size(max = 500, message = "Message cannot exceed 500 characters")
    private String senderNote;
}