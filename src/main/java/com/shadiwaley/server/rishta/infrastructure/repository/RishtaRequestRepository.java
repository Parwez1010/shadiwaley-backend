package com.shadiwaley.server.rishta.infrastructure.repository;

import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RishtaRequestRepository extends JpaRepository<RishtaRequest, UUID> {

    /*
     * Backward-compatible methods used by existing services.
     * Explicit JPQL is safer because senderUser/receiverUser are entity relations,
     * not direct UUID columns.
     */

    @Query("""
        select r
        from RishtaRequest r
        where r.senderUser.id = :senderUserId
        order by r.createdAt desc
    """)
    List<RishtaRequest> findBySenderUserIdOrderByCreatedAtDesc(
            @Param("senderUserId") UUID senderUserId
    );

    @Query("""
        select r
        from RishtaRequest r
        where r.receiverUser.id = :receiverUserId
        order by r.createdAt desc
    """)
    List<RishtaRequest> findByReceiverUserIdOrderByCreatedAtDesc(
            @Param("receiverUserId") UUID receiverUserId
    );

    @Query("""
        select r
        from RishtaRequest r
        where r.senderUser.id = :senderUserId
    """)
    List<RishtaRequest> findBySenderUserId(
            @Param("senderUserId") UUID senderUserId
    );

    @Query("""
        select r
        from RishtaRequest r
        where r.senderUser.id = :senderUserId
          and r.receiverUser.id = :receiverUserId
          and r.status in :statuses
    """)
    Optional<RishtaRequest> findBySenderUserIdAndReceiverUserIdAndStatusIn(
            @Param("senderUserId") UUID senderUserId,
            @Param("receiverUserId") UUID receiverUserId,
            @Param("statuses") List<RishtaRequestStatus> statuses
    );

    @Query("""
        select count(r) > 0
        from RishtaRequest r
        where r.senderUser.id = :senderUserId
          and r.receiverUser.id = :receiverUserId
          and r.status = :status
    """)
    boolean existsBySenderUserIdAndReceiverUserIdAndStatus(
            @Param("senderUserId") UUID senderUserId,
            @Param("receiverUserId") UUID receiverUserId,
            @Param("status") RishtaRequestStatus status
    );

    long countByStatus(RishtaRequestStatus status);

    List<RishtaRequest> findByStatusAndExpiresAtBefore(
            RishtaRequestStatus status,
            Instant now
    );

    /*
     * Optimized fetch-join methods for customer proposal APIs.
     * These avoid LazyInitializationException and N+1 queries.
     */

    @Query("""
        select distinct r
        from RishtaRequest r
        left join fetch r.senderUser
        left join fetch r.receiverUser
        left join fetch r.senderProfile
        left join fetch r.receiverProfile
        where r.senderUser.id = :senderUserId
          and (:status is null or r.status = :status)
        order by r.createdAt desc
    """)
    List<RishtaRequest> findSentWithDetailsAndOptionalStatus(
            @Param("senderUserId") UUID senderUserId,
            @Param("status") RishtaRequestStatus status
    );

    @Query("""
        select distinct r
        from RishtaRequest r
        left join fetch r.senderUser
        left join fetch r.receiverUser
        left join fetch r.senderProfile
        left join fetch r.receiverProfile
        where r.receiverUser.id = :receiverUserId
          and (:status is null or r.status = :status)
        order by r.createdAt desc
    """)
    List<RishtaRequest> findReceivedWithDetailsAndOptionalStatus(
            @Param("receiverUserId") UUID receiverUserId,
            @Param("status") RishtaRequestStatus status
    );

    @Query("""
        select distinct r
        from RishtaRequest r
        left join fetch r.senderUser
        left join fetch r.receiverUser
        left join fetch r.senderProfile
        left join fetch r.receiverProfile
        where r.id = :requestId
    """)
    Optional<RishtaRequest> findByIdWithDetails(
            @Param("requestId") UUID requestId
    );
}