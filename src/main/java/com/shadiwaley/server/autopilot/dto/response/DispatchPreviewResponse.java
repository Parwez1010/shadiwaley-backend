package com.shadiwaley.server.autopilot.dto.response;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchChannel;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DispatchPreviewResponse {

    private UUID draftId;
    private UUID queueId;
    private UUID sourceProfileId;

    private AutopilotDispatchChannel channel;

    private boolean shareProfilePhoto;

    private String messagePreview;

    private List<DispatchPreviewItemResponse> items;

    private List<String> warnings;

    private boolean canSend;

    private String blockReason;
}