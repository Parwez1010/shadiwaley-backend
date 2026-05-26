package com.shadiwaley.server.chat.dto.admin.request;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminChatStatusUpdateRequest {

    @NotNull
    private ChatRoomStatus status;

    private String note;
}