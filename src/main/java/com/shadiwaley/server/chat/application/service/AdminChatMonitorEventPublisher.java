package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.chat.dto.websocket.AdminChatMonitorEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminChatMonitorEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(AdminChatMonitorEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/admin.chat.monitor",
                event
        );
    }
}