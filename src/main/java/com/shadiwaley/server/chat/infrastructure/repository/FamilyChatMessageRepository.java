package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatMessage;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface FamilyChatMessageRepository extends JpaRepository<FamilyChatMessage, UUID> {

    List<FamilyChatMessage> findByRoomIdAndDeletedAtIsNullOrderBySentAtAsc(UUID roomId, Pageable pageable);

    List<FamilyChatMessage> findByRoomIdAndDeletedAtIsNullOrderBySentAtDesc(UUID roomId, Pageable pageable);

    List<FamilyChatMessage> findByRoomIdAndSentAtBeforeAndDeletedAtIsNullOrderBySentAtDesc(
            UUID roomId,
            Instant before,
            Pageable pageable
    );

    long count();

    long countByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(UUID roomId, UUID senderUserId);

    List<FamilyChatMessage> findByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(UUID roomId, UUID senderUserId);

    Page<FamilyChatMessage> findByRoomIdOrderBySentAtDesc(UUID roomId, Pageable pageable);

    long countByRoomId(UUID roomId);

    long countByRoomIdAndMediaFileIsNotNull(UUID roomId);


    long countByRoomIdAndReadAtIsNull(UUID roomId);

    long countBySentAtBetween(Instant from, Instant to);

    @Query("""
            select count(m)
            from FamilyChatMessage m
            where m.room.id = :roomId
              and m.senderUser.id <> :senderUserId
              and m.readAt is null
            """)
    long countUnreadForOtherSide(
            @Param("roomId") UUID roomId,
            @Param("senderUserId") UUID senderUserId
    );

    Page<FamilyChatMessage> findByRoomIdAndSentAtBeforeOrderBySentAtDesc(
            UUID roomId,
            Instant sentAt,
            Pageable pageable
    );

    Page<FamilyChatMessage> findByRoomIdAndSentAtAfterOrderBySentAtAsc(
            UUID roomId,
            Instant sentAt,
            Pageable pageable
    );


}