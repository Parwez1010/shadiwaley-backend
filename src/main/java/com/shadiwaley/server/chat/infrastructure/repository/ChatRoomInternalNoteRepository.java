package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.ChatRoomInternalNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatRoomInternalNoteRepository extends JpaRepository<ChatRoomInternalNote, UUID> {

    List<ChatRoomInternalNote> findByRoomIdOrderByCreatedAtDesc(UUID roomId);
}