package com.shadiwaley.server.support.infrastructure.repository;

import com.shadiwaley.server.support.infrastructure.entity.SupportTicketReply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SupportTicketReplyRepository extends JpaRepository<SupportTicketReply, UUID> {

    List<SupportTicketReply> findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(
            UUID ticketId
    );

    List<SupportTicketReply> findByTicketIdOrderByCreatedAtAsc(
            UUID ticketId
    );
}