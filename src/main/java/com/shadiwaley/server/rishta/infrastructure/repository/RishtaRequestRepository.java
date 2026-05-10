package com.shadiwaley.server.rishta.infrastructure.repository;

import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RishtaRequestRepository
        extends JpaRepository<RishtaRequest, UUID> {

    List<RishtaRequest> findBySenderUserIdOrderByCreatedAtDesc(UUID senderUserId);

    List<RishtaRequest> findByReceiverUserIdOrderByCreatedAtDesc(UUID receiverUserId);

    Optional<RishtaRequest> findBySenderUserIdAndReceiverUserIdAndStatusIn(
            UUID senderUserId,
            UUID receiverUserId,
            List<RishtaRequestStatus> statuses
    );
    long countByStatus(com.shadiwaley.server.rishta.domain.RishtaRequestStatus status);

    boolean existsBySenderUserIdAndReceiverUserIdAndStatus(
            UUID senderUserId,
            UUID receiverUserId,
            RishtaRequestStatus status
    );

    List<RishtaRequest> findByStatusAndExpiresAtBefore(
            RishtaRequestStatus status,
            Instant now
    );

}