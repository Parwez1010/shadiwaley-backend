package com.shadiwaley.server.proposal.infrastructure.repository;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ProposalRepository extends JpaRepository<Proposal, UUID>, JpaSpecificationExecutor<Proposal> {

    Optional<Proposal> findTopByFromProfileIdAndToProfileIdOrderByCreatedAtDesc(
            UUID fromProfileId,
            UUID toProfileId
    );

    boolean existsByFromProfileIdAndToProfileIdAndStatusIn(
            UUID fromProfileId,
            UUID toProfileId,
            Collection<ProposalStatus> statuses
    );

    boolean existsByToProfileIdAndFromProfileIdAndStatusIn(
            UUID toProfileId,
            UUID fromProfileId,
            Collection<ProposalStatus> statuses
    );

    boolean existsByFromProfileIdAndToProfileIdAndStatusInAndDispatchedAtAfter(
            UUID fromProfileId,
            UUID toProfileId,
            Collection<ProposalStatus> statuses,
            Instant after
    );

    boolean existsByToProfileIdAndFromProfileIdAndStatusInAndDispatchedAtAfter(
            UUID toProfileId,
            UUID fromProfileId,
            Collection<ProposalStatus> statuses,
            Instant after
    );

    @Query("""
        SELECT p
        FROM Proposal p
        WHERE (
            p.fromProfile.id = :sourceId
            AND p.toProfile.id = :targetId
        )
        OR (
            p.fromProfile.id = :targetId
            AND p.toProfile.id = :sourceId
        )
        ORDER BY p.createdAt DESC
        """)
    Optional<Proposal> findTopByProfiles(
            @Param("sourceId") UUID sourceId,
            @Param("targetId") UUID targetId
    );
}