package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.chat.domain.ChatSocketEventType;
import com.shadiwaley.server.chat.dto.websocket.ChatWebSocketEvent;
import com.shadiwaley.server.chat.dto.websocket.PresenceEvent;
import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatRoom;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatPresencePublisherService {

    private final FamilyChatRoomRepository chatRoomRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceService presenceService;

    public void publishOnline(UUID userId) {
        presenceService.markOnline(userId);

        publishPresenceToUserRooms(
                userId,
                true,
                null,
                ChatSocketEventType.USER_ONLINE
        );
    }

    public void publishOffline(UUID userId) {
        presenceService.markOffline(userId);

        publishPresenceToUserRooms(
                userId,
                false,
                presenceService.getLastSeen(userId),
                ChatSocketEventType.USER_OFFLINE
        );
    }

    private void publishPresenceToUserRooms(
            UUID userId,
            boolean online,
            Instant lastSeenAt,
            String eventType
    ) {
        List<FamilyChatRoom> rooms =
                chatRoomRepository.findByBoyUserIdOrGirlUserId(
                        userId,
                        userId
                );

        PresenceEvent payload = PresenceEvent.builder()
                .userId(userId)
                .online(online)
                .lastSeenAt(lastSeenAt)
                .build();

        rooms.forEach(room ->
                messagingTemplate.convertAndSend(
                        "/topic/chat.room." + room.getId(),
                        ChatWebSocketEvent.builder()
                                .event(eventType)
                                .roomId(room.getId())
                                .payload(payload)
                                .emittedAt(Instant.now())
                                .build()
                )
        );
    }
}