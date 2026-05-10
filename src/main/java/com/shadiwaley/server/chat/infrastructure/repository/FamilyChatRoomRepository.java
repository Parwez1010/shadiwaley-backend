package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FamilyChatRoomRepository extends JpaRepository<FamilyChatRoom, UUID> {

    Optional<FamilyChatRoom> findByRishtaRequestId(UUID rishtaRequestId);

    List<FamilyChatRoom> findByBoyUserIdOrGirlUserIdOrderByUpdatedAtDesc(
            UUID boyUserId,
            UUID girlUserId
    );
    long countByStatus(com.shadiwaley.server.chat.domain.ChatRoomStatus status);
}