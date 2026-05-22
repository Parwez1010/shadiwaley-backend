package com.shadiwaley.server.autopilot.dto.request;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateDispatchPreviewRequest {

    @NotNull
    private UUID queueId;

    @NotNull
    private UUID sourceProfileId;

    @NotEmpty
    private List<UUID> candidateProfileIds;

    @NotNull
    private AutopilotDispatchChannel channel;

    private boolean shareProfilePhoto = true;

    private String note;
}