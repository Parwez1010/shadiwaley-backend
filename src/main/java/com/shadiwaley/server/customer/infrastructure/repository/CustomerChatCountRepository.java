package com.shadiwaley.server.customer.infrastructure.repository;

import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CustomerChatCountRepository extends JpaRepository<FamilyChatMessage, UUID> {

    @Query(
            value = """
            SELECT COUNT(m.id)
            FROM family_chat_message m
            JOIN family_chat_room r ON r.id = m.room_id
            WHERE (
                r.boy_user_id = CAST(:userId AS uuid)
                OR r.girl_user_id = CAST(:userId AS uuid)
            )
            AND m.sender_user_id <> CAST(:userId AS uuid)
            AND m.read_at IS NULL
            AND m.deleted_at IS NULL
            """,
            nativeQuery = true
    )
    long countUnreadForUser(@Param("userId") UUID userId);
}