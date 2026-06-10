package com.shadiwaley.server.chat.api.websocket;

import com.shadiwaley.server.chat.application.service.ChatPresencePublisherService;
import com.shadiwaley.server.chat.application.service.ChatPresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketPresenceListener {

    private final ChatPresencePublisherService presencePublisherService;

    @EventListener
    public void handleConnect(
            SessionConnectedEvent event
    ) {
        Principal principal =
                StompHeaderAccessor.wrap(
                        event.getMessage()
                ).getUser();

        if (principal == null) {
            return;
        }

        UUID actorId =
                UUID.fromString(principal.getName());

        presencePublisherService.publishOnline(actorId);
    }

    @EventListener
    public void handleDisconnect(
            SessionDisconnectEvent event
    ) {
        Principal principal =
                StompHeaderAccessor.wrap(
                        event.getMessage()
                ).getUser();

        if (principal == null) {
            return;
        }

        UUID actorId =
                UUID.fromString(principal.getName());

        presencePublisherService.publishOffline(actorId);
    }
}