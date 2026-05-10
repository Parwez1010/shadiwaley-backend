package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.ChatMessageReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageReportRepository
        extends JpaRepository<ChatMessageReport, UUID> {
}