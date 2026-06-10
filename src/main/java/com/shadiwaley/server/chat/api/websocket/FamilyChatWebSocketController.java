package com.shadiwaley.server.chat.api.websocket;

import com.shadiwaley.server.chat.application.service.AdminChatMonitorService;
import com.shadiwaley.server.chat.application.service.FamilyChatService;
import com.shadiwaley.server.chat.dto.admin.request.AdminSendChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.SendChatMessageRequest;
import com.shadiwaley.server.chat.dto.response.ChatMessageResponse;
import com.shadiwaley.server.security.ActorType;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FamilyChatWebSocketController {

    private final FamilyChatService familyChatService;
    private final AdminChatMonitorService adminChatMonitorService;


    @MessageMapping("/chat.send/{roomId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable UUID roomId,
            SendChatMessageRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getDetails() != ActorType.CUSTOMER) {
            throw new IllegalArgumentException("Only customers can send family chat messages");
        }

        UUID senderUserId = UUID.fromString(authentication.getPrincipal().toString());

        return familyChatService.sendMessageFromWebSocket(
                roomId,
                senderUserId,
                request
        );
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void typing(
            @DestinationVariable UUID roomId,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getDetails() != ActorType.CUSTOMER) {
            throw new IllegalArgumentException("Only customers can send typing events");
        }

        UUID senderUserId = UUID.fromString(authentication.getPrincipal().toString());

        familyChatService.sendTyping(roomId, senderUserId);
    }

    @MessageMapping("/admin.chat.send/{roomId}")
    public ChatMessageResponse sendAdminMessage(
            @DestinationVariable UUID roomId,
            AdminSendChatMessageRequest request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getDetails() != ActorType.EMPLOYEE) {
            throw new IllegalArgumentException("Only employees can send CRM chat messages");
        }

        return adminChatMonitorService.sendCrmMessage(roomId, request);
    }



}