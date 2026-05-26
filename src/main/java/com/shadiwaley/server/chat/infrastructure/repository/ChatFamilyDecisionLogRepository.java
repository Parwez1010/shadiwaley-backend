package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.ChatFamilyDecisionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatFamilyDecisionLogRepository extends JpaRepository<ChatFamilyDecisionLog, UUID> {

    List<ChatFamilyDecisionLog> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    Page<ChatFamilyDecisionLog> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);

}