package com.shadiwaley.server.communication.application.service;

import com.shadiwaley.server.communication.dto.websocket.CommunicationCenterEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunicationCenterEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publish(CommunicationCenterEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/admin.communication-center",
                event
        );

        if (event.getAssignedEmployeeId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/admin.communication-center.employee." + event.getAssignedEmployeeId(),
                    event
            );
        }
    }
}