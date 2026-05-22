package com.shadiwaley.server.autopilot.infrastructure.repository;

import com.shadiwaley.server.autopilot.domain.AutopilotDispatchItemStatus;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AutopilotDispatchItemRepository
        extends JpaRepository<AutopilotDispatchItem, UUID> {

    boolean existsBySourceProfile_IdAndCandidateProfile_Id(
            UUID sourceProfileId,
            UUID candidateProfileId
    );

    boolean existsBySourceProfile_IdAndCandidateProfile_IdAndStatusIn(
            UUID sourceProfileId,
            UUID candidateProfileId,
            Collection<AutopilotDispatchItemStatus> statuses
    );

    @EntityGraph(attributePaths = {
            "candidateProfile",
            "candidateProfile.userAccount",
            "proposal"
    })
    List<AutopilotDispatchItem> findByDispatchBatch_Id(UUID dispatchBatchId);

    @Query("""
            SELECT COUNT(i)
            FROM AutopilotDispatchItem i
            WHERE i.dispatchBatch.id = :batchId
            AND i.responseStatus = :responseStatus
            """)
    long countByBatchAndResponse(
            @Param("batchId") UUID batchId,
            @Param("responseStatus") com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus responseStatus
    );
}