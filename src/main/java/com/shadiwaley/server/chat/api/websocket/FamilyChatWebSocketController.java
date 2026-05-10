package com.shadiwaley.server.chat.api.websocket;

import com.shadiwaley.server.chat.application.service.FamilyChatService;
import com.shadiwaley.server.chat.dto.request.SendChatMessageRequest;
import com.shadiwaley.server.chat.dto.response.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class FamilyChatWebSocketController {

    private final FamilyChatService familyChatService;

    @MessageMapping("/chat.send/{roomId}")
    public ChatMessageResponse sendMessage(
            @DestinationVariable UUID roomId,
            SendChatMessageRequest request,
            Authentication authentication
    ) {
        UUID senderUserId = UUID.fromString(authentication.getPrincipal().toString());
        return familyChatService.sendMessageFromWebSocket(roomId, senderUserId, request);
    }

    @MessageMapping("/chat.typing/{roomId}")
    public void typing(
            @DestinationVariable UUID roomId,
            Authentication authentication
    ) {

        UUID senderUserId = UUID.fromString(authentication.getPrincipal().toString());

        familyChatService.sendTyping(roomId, senderUserId);
    }

}