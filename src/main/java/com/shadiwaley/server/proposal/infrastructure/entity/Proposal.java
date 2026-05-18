package com.shadiwaley.server.proposal.infrastructure.entity;

import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "proposal")
public class Proposal {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_profile_id", nullable = false)
    private UserProfile fromProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_profile_id", nullable = false)
    private UserProfile toProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crm_case_id")
    private CrmCase crmCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProposalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_channel", nullable = false, length = 50)
    private ProposalDispatchChannel dispatchChannel;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "match_score")
    private Integer matchScore;

    @Column(name = "share_profile_photo", nullable = false)
    private boolean shareProfilePhoto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by_employee_id")
    private EmployeeAccount dispatchedByEmployee;

    @Column(name = "dispatched_by_name", length = 150)
    private String dispatchedByName;

    @Column(name = "dispatched_at", nullable = false)
    private Instant dispatchedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_updated_by_employee_id")
    private EmployeeAccount lastUpdatedByEmployee;

    @Column(name = "last_updated_by_name", length = 150)
    private String lastUpdatedByName;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", length = 50)
    private ProposalSourceType sourceType;


    @Column(name = "last_updated_at")
    private Instant lastUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();

        if (id == null) id = UUID.randomUUID();
        if (status == null) status = ProposalStatus.SENT;
        if (dispatchedAt == null) dispatchedAt = now;

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}