package com.shadiwaley.server.chat.infrastructure.repository;

import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatRoom;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.*;

public interface FamilyChatRoomRepository extends JpaRepository<FamilyChatRoom, UUID>, JpaSpecificationExecutor<FamilyChatRoom> {

    Optional<FamilyChatRoom> findByRishtaRequestId(UUID rishtaRequestId);

    List<FamilyChatRoom> findByBoyUserIdOrGirlUserIdOrderByUpdatedAtDesc(UUID boyUserId, UUID girlUserId);

    long countByStatus(ChatRoomStatus status);

    long countByAssignedEmployeeIdAndStatus(UUID assignedEmployeeId, ChatRoomStatus status);

    long countByAssignedEmployeeId(UUID assignedEmployeeId);

    long countByReportedTrue();

    long countByNeedsAttentionTrue();

    long countByBlockedTrue();

    long countByLastMessageAtBetween(Instant from, Instant to);

    @Query("""
            select coalesce(sum(r.messageCount), 0)
            from FamilyChatRoom r
            where (:assignedEmployeeId is null or r.assignedEmployee.id = :assignedEmployeeId)
              and (:fromDate is null or r.createdAt >= :fromDate)
              and (:toDate is null or r.createdAt < :toDate)
            """)
    long sumMessageCount(
            @Param("assignedEmployeeId") UUID assignedEmployeeId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );
}