package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.ChatMonitorNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMonitorNoteRepository extends JpaRepository<ChatMonitorNote, UUID> {

    List<ChatMonitorNote> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
    Page<ChatMonitorNote> findByRoomIdOrderByCreatedAtDesc(UUID roomId, Pageable pageable);


}