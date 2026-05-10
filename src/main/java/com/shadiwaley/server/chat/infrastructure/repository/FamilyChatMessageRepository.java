package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FamilyChatMessageRepository extends JpaRepository<FamilyChatMessage, UUID> {

    List<FamilyChatMessage> findByRoomIdAndDeletedAtIsNullOrderBySentAtAsc(
            UUID roomId,
            Pageable pageable
    );

    List<FamilyChatMessage> findByRoomIdAndDeletedAtIsNullOrderBySentAtDesc(
            UUID roomId,
            Pageable pageable
    );

    List<FamilyChatMessage> findByRoomIdAndSentAtBeforeAndDeletedAtIsNullOrderBySentAtDesc(
            UUID roomId,
            Instant before,
            Pageable pageable
    );

    long countByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(
            UUID roomId,
            UUID senderUserId
    );

    List<FamilyChatMessage> findByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(
            UUID roomId,
            UUID senderUserId
    );
}