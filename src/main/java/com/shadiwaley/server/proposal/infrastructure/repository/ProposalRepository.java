package com.shadiwaley.server.proposal.infrastructure.repository;

import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
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

    Page<Proposal> findByStatusOrderByUpdatedAtDesc(
            ProposalStatus status,
            Pageable pageable
    );

    @Query("""
        SELECT p
        FROM Proposal p
        WHERE p.crmCase.assignedEmployee.id = :employeeId
        ORDER BY p.updatedAt DESC
        """)
    Page<Proposal> findAssignedProposals(
            @Param("employeeId") UUID employeeId,
            Pageable pageable
    );

    @Query(
            value = """
        SELECT DISTINCT p.*
        FROM proposal p
        LEFT JOIN crm_case c ON c.id = p.crm_case_id
        LEFT JOIN user_profile fp ON fp.id = p.from_profile_id
        LEFT JOIN user_account fu ON fu.id = fp.user_account_id
        LEFT JOIN user_profile tp ON tp.id = p.to_profile_id
        LEFT JOIN user_account tu ON tu.id = tp.user_account_id
        LEFT JOIN parent_profile fparent ON fparent.user_account_id = fp.user_account_id
        LEFT JOIN parent_profile tparent ON tparent.user_account_id = tp.user_account_id
        WHERE (:status IS NULL OR p.status = CAST(:status AS varchar))
        AND (:crmEmployeeId IS NULL OR c.assigned_employee_id = CAST(:crmEmployeeId AS uuid))
        AND (:fromProfileId IS NULL OR p.from_profile_id = CAST(:fromProfileId AS uuid))
        AND (:toProfileId IS NULL OR p.to_profile_id = CAST(:toProfileId AS uuid))
        AND (
            :district IS NULL
            OR LOWER(COALESCE(fparent.district, '')) = LOWER(CAST(:district AS varchar))
            OR LOWER(COALESCE(tparent.district, '')) = LOWER(CAST(:district AS varchar))
        )
        AND (
            :side IS NULL
            OR LOWER(CAST(fu.side AS varchar)) = LOWER(CAST(:side AS varchar))
            OR LOWER(CAST(tu.side AS varchar)) = LOWER(CAST(:side AS varchar))
        )
        AND (CAST(:fromDate AS timestamp) IS NULL OR p.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR p.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(fp.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tp.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(fparent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tparent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(fu.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tu.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(p.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        ORDER BY p.updated_at DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT p.id)
        FROM proposal p
        LEFT JOIN crm_case c ON c.id = p.crm_case_id
        LEFT JOIN user_profile fp ON fp.id = p.from_profile_id
        LEFT JOIN user_account fu ON fu.id = fp.user_account_id
        LEFT JOIN user_profile tp ON tp.id = p.to_profile_id
        LEFT JOIN user_account tu ON tu.id = tp.user_account_id
        LEFT JOIN parent_profile fparent ON fparent.user_account_id = fp.user_account_id
        LEFT JOIN parent_profile tparent ON tparent.user_account_id = tp.user_account_id
        WHERE (:status IS NULL OR p.status = CAST(:status AS varchar))
        AND (:crmEmployeeId IS NULL OR c.assigned_employee_id = CAST(:crmEmployeeId AS uuid))
        AND (:fromProfileId IS NULL OR p.from_profile_id = CAST(:fromProfileId AS uuid))
        AND (:toProfileId IS NULL OR p.to_profile_id = CAST(:toProfileId AS uuid))
        AND (
            :district IS NULL
            OR LOWER(COALESCE(fparent.district, '')) = LOWER(CAST(:district AS varchar))
            OR LOWER(COALESCE(tparent.district, '')) = LOWER(CAST(:district AS varchar))
        )
        AND (
            :side IS NULL
            OR LOWER(CAST(fu.side AS varchar)) = LOWER(CAST(:side AS varchar))
            OR LOWER(CAST(tu.side AS varchar)) = LOWER(CAST(:side AS varchar))
        )
        AND (CAST(:fromDate AS timestamp) IS NULL OR p.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR p.created_at <= CAST(:toDate AS timestamp))
        AND (
            :search IS NULL
            OR LOWER(COALESCE(fp.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tp.candidate_first_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(fparent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tparent.parent_name, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(fu.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR LOWER(COALESCE(tu.phone, '')) LIKE LOWER(CONCAT('%', CAST(:search AS varchar), '%'))
            OR CAST(p.id AS varchar) LIKE CONCAT('%', CAST(:search AS varchar), '%')
        )
        """,
            nativeQuery = true
    )
    Page<Proposal> searchPipeline(
            @Param("search") String search,
            @Param("status") String status,
            @Param("crmEmployeeId") UUID crmEmployeeId,
            @Param("fromProfileId") UUID fromProfileId,
            @Param("toProfileId") UUID toProfileId,
            @Param("district") String district,
            @Param("side") String side,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            Pageable pageable
    );

    @Query(value = """
        SELECT p.*
        FROM proposal p
        LEFT JOIN crm_case c ON c.id = p.crm_case_id
        WHERE (:crmEmployeeId IS NULL OR c.assigned_employee_id = CAST(:crmEmployeeId AS uuid))
        AND (CAST(:fromDate AS timestamp) IS NULL OR p.created_at >= CAST(:fromDate AS timestamp))
        AND (CAST(:toDate AS timestamp) IS NULL OR p.created_at <= CAST(:toDate AS timestamp))
        ORDER BY p.updated_at DESC
        """, nativeQuery = true)
    List<Proposal> findPipelineSummarySource(
            @Param("crmEmployeeId") UUID crmEmployeeId,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );


    boolean existsByFromProfile_IdAndToProfile_Id(UUID fromProfileId, UUID toProfileId);

    boolean existsByToProfile_IdAndFromProfile_Id(UUID toProfileId, UUID fromProfileId);


    Optional<Proposal> findTopByToProfileIdAndFromProfileIdOrderByCreatedAtDesc(
            UUID toProfileId,
            UUID fromProfileId
    );
}