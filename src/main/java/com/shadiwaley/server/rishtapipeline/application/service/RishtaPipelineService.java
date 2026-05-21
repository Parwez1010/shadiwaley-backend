package com.shadiwaley.server.rishtapipeline.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.audit.infrastructure.repository.AuditLogRepository;
import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseTimelineRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmFollowUpRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmTimelineRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.dto.response.ProposalDetailResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalResponse;
import com.shadiwaley.server.proposal.dto.response.ProposalStatusHistoryResponse;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.entity.ProposalStatusHistory;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalStatusHistoryRepository;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import com.shadiwaley.server.rishtapipeline.dto.request.AddRishtaPipelineNoteRequest;
import com.shadiwaley.server.rishtapipeline.dto.request.CreatePipelineFollowUpRequest;
import com.shadiwaley.server.rishtapipeline.dto.request.UpdateRishtaPipelineStageRequest;
import com.shadiwaley.server.rishtapipeline.dto.response.*;
import com.shadiwaley.server.rishtapipeline.infrastructure.entity.RishtaPipelineNote;
import com.shadiwaley.server.rishtapipeline.infrastructure.repository.RishtaPipelineNoteRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RishtaPipelineService {

    private final ProposalRepository proposalRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final CrmFollowUpRepository crmFollowUpRepository;
    private final RishtaPipelineNoteRepository noteRepository;

    private final RevenueService revenueService;

    private final RishtaPipelineMapper mapper;
    private final RishtaPipelinePermissionService permissionService;
    private final RishtaPipelineStageResolver stageResolver;
    private  final AuditLogService auditLogService;
    private final CrmTimelineRepository crmTimelineRepository;
    private final ProposalStatusHistoryRepository proposalStatusHistoryRepository;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;




    @Transactional(readOnly = true)
    public RishtaPipelinePageResponse getPipeline(
            int page,
            int size,
            String search,
            RishtaPipelineStage stage,
            ProposalStatus status,
            UUID crmEmployeeId,
            UUID fromProfileId,
            UUID toProfileId,
            String district,
            String side,
            String planCode,
            String paymentStatus,
            String subscriptionStatus,
            Boolean overdueOnly,
            LocalDate fromDate,
            LocalDate toDate,
            String sort
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        Page<Proposal> proposals;

        UUID effectiveCrmEmployeeId = crmEmployeeId;

        if (!permissionService.isAdmin(employee)) {
            effectiveCrmEmployeeId = employee.getId();
        }

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        proposals = proposalRepository.searchPipeline(
                isBlank(search) ? null : search.trim(),
                status != null ? status.name() : null,
                effectiveCrmEmployeeId,
                fromProfileId,
                toProfileId,
                isBlank(district) ? null : district.trim(),
                isBlank(side) ? null : side.trim(),
                from,
                to,
                pageable
        );

        List<RishtaPipelineItemResponse> items = proposals.getContent()
                .stream()
                .map(this::mapProposal)
                .filter(item -> stage == null || item.getPipelineStage() == stage)
                .filter(item -> isBlank(planCode) || equalsIgnoreCase(item.getFromPlanName(), planCode))
                .filter(item -> isBlank(paymentStatus) || equalsIgnoreCase(item.getFromPaymentStatus(), paymentStatus))
                .filter(item -> isBlank(subscriptionStatus) || equalsIgnoreCase(item.getFromSubscriptionStatus(), subscriptionStatus))
                .filter(item -> !Boolean.TRUE.equals(overdueOnly) || item.isOverdue())
                .toList();

        return RishtaPipelinePageResponse.builder()
                .items(items)
                .page(proposals.getNumber())
                .size(proposals.getSize())
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : 1)
                .last(true)
                .build();
    }
    @Transactional(readOnly = true)
    public RishtaPipelineItemResponse getPipelineItem(UUID proposalId) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        permissionService.assertCanViewProposal(proposal);

        return mapProposal(proposal);
    }

    private RishtaPipelineItemResponse mapProposal(Proposal proposal) {

        ParentProfile fromParent =
                proposal.getFromProfile() != null
                        ? parentProfileRepository
                        .findByUserAccountId(
                                proposal.getFromProfile()
                                        .getUserAccount()
                                        .getId()
                        )
                        .orElse(null)
                        : null;

        ParentProfile toParent =
                proposal.getToProfile() != null
                        ? parentProfileRepository
                        .findByUserAccountId(
                                proposal.getToProfile()
                                        .getUserAccount()
                                        .getId()
                        )
                        .orElse(null)
                        : null;

        CrmFollowUp followUp =
                crmFollowUpRepository
                        .findTopByProposal_IdOrderByScheduledAtDesc(proposal.getId())
                        .orElse(null);

        RishtaPipelineNote note =
                noteRepository
                        .findTopByProposalIdOrderByCreatedAtDesc(proposal.getId())
                        .orElse(null);

        SubscriptionResponse subscription =
                proposal.getFromProfile() != null
                        ? revenueService
                        .getFamilySubscription(
                                proposal.getFromProfile()
                                        .getUserAccount()
                                        .getId()
                        )
                        .getCurrentSubscription()
                        : null;

        return mapper.toResponse(
                proposal,
                fromParent,
                toParent,
                followUp,
                note,
                subscription
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isBlank();
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        String normalized = sort.trim().toLowerCase();

        if (normalized.contains("createdat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "createdAt")
                    : Sort.by(Sort.Direction.DESC, "createdAt");
        }

        if (normalized.contains("sentat") || normalized.contains("dispatchedat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "dispatchedAt")
                    : Sort.by(Sort.Direction.DESC, "dispatchedAt");
        }

        return Sort.by(Sort.Direction.DESC, "updatedAt");
    }

    @Transactional(readOnly = true)
    public RishtaPipelineSummaryResponse getSummary(
            LocalDate fromDate,
            LocalDate toDate,
            UUID crmEmployeeId
    ) {
        EmployeeAccount employee = permissionService.getCurrentEmployee();

        UUID effectiveCrmEmployeeId = crmEmployeeId;

        if (!permissionService.isAdmin(employee)) {
            effectiveCrmEmployeeId = employee.getId();
        }

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        List<Proposal> proposals = proposalRepository.findPipelineSummarySource(
                effectiveCrmEmployeeId,
                from,
                to
        );

        long total = proposals.size();

        long sent = 0;
        long viewed = 0;
        long interested = 0;
        long discussion = 0;
        long accepted = 0;
        long notInterested = 0;
        long rejected = 0;
        long closed = 0;
        long overdue = 0;

        for (Proposal proposal : proposals) {
            RishtaPipelineItemResponse item = mapProposal(proposal);

            switch (item.getPipelineStage()) {
                case SENT -> sent++;
                case VIEWED -> viewed++;
                case INTERESTED -> interested++;
                case DISCUSSION -> discussion++;
                case ACCEPTED -> accepted++;
                case NOT_INTERESTED -> notInterested++;
                case REJECTED -> rejected++;
                case CLOSED -> closed++;
            }

            if (item.isOverdue()) {
                overdue++;
            }
        }

        BigDecimal conversionRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(accepted)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        BigDecimal interestRate = total == 0
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(interested + discussion + accepted)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);

        return RishtaPipelineSummaryResponse.builder()
                .total(total)
                .sent(sent)
                .viewed(viewed)
                .interested(interested)
                .discussion(discussion)
                .accepted(accepted)
                .notInterested(notInterested)
                .rejected(rejected)
                .closed(closed)
                .overdue(overdue)
                .conversionRate(conversionRate)
                .interestRate(interestRate)
                .build();
    }

    @Transactional(readOnly = true)
    public RishtaPipelineKanbanResponse getKanban(
            LocalDate fromDate,
            LocalDate toDate,
            UUID crmEmployeeId
    ) {
        EmployeeAccount employee =
                permissionService.getCurrentEmployee();

        UUID effectiveCrmEmployeeId = crmEmployeeId;

        if (!permissionService.isAdmin(employee)) {
            effectiveCrmEmployeeId = employee.getId();
        }

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                : null;

        List<Proposal> proposals =
                proposalRepository.findPipelineSummarySource(
                        effectiveCrmEmployeeId,
                        from,
                        to
                );

        List<RishtaPipelineItemResponse> mapped =
                proposals.stream()
                        .map(this::mapProposal)
                        .toList();

        List<RishtaPipelineKanbanColumnResponse> columns =
                Arrays.stream(RishtaPipelineStage.values())
                        .map(stage -> buildKanbanColumn(stage, mapped))
                        .toList();

        return RishtaPipelineKanbanResponse.builder()
                .columns(columns)
                .build();
    }

    private RishtaPipelineKanbanColumnResponse buildKanbanColumn(
            RishtaPipelineStage stage,
            List<RishtaPipelineItemResponse> items
    ) {

        List<RishtaPipelineItemResponse> stageItems =
                items.stream()
                        .filter(i -> i.getPipelineStage() == stage)
                        .sorted(
                                Comparator.comparing(
                                        RishtaPipelineItemResponse::getLastActivityAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                        )
                        .limit(20)
                        .toList();

        return RishtaPipelineKanbanColumnResponse.builder()
                .stage(stage)
                .label(stageResolver.label(stage))
                .count(stageItems.size())
                .items(stageItems)
                .build();
    }

    @Transactional
    public RishtaPipelineStageUpdateResponse updateStage(
            UUID proposalId,
            UpdateRishtaPipelineStageRequest request
    ) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        permissionService.assertCanUpdateProposal(proposal);

        ProposalStatus oldStatus = proposal.getStatus();

        proposal.setStatus(request.getStatus());
        proposal.setUpdatedAt(Instant.now());

        Proposal saved = proposalRepository.save(proposal);

        createProposalStatusHistory(
                saved,
                request.getStatus(),
                request.getNote()
        );

        createPipelineTimelineEvent(
                saved,
                oldStatus,
                request.getStatus(),
                request.getNote()
        );

        createPipelineAuditEvent(
                saved,
                oldStatus,
                request.getStatus()
        );

        if (!isBlank(request.getNote())) {
            createPipelineNote(saved, request.getNote());
        }

        if (request.getNextFollowUpAt() != null) {
            createProposalFollowUp(saved, request.getNextFollowUpAt(), request.getNote());
        }


        var stage = stageResolver.resolve(saved.getStatus());

        return RishtaPipelineStageUpdateResponse.builder()
                .proposalId(saved.getId())
                .status(saved.getStatus())
                .pipelineStage(stage)
                .pipelineStageLabel(stageResolver.label(stage))
                .lastStatusAt(saved.getUpdatedAt())
                .nextFollowUpAt(request.getNextFollowUpAt())
                .build();
    }

    private RishtaPipelineNote createPipelineNote(
            Proposal proposal,
            String noteText
    ) {
        if (isBlank(noteText)) {
            throw new IllegalArgumentException("Note cannot be empty");
        }

        EmployeeAccount employee =
                permissionService.getCurrentEmployee();

        RishtaPipelineNote note = new RishtaPipelineNote();
        note.setProposal(proposal);
        note.setNote(noteText.trim());
        note.setCreatedByEmployee(employee);
        note.setCreatedByName(employee != null ? employee.getFullName() : "System");

        return noteRepository.save(note);
    }

    private void createProposalFollowUp(
            Proposal proposal,
            Instant nextFollowUpAt,
            String note
    ) {
        if (proposal.getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee =
                permissionService.getCurrentEmployee();

        CrmFollowUp followUp = new CrmFollowUp();
        followUp.setCrmCase(proposal.getCrmCase());
        followUp.setProposal(proposal);
        followUp.setAssignedEmployee(
                proposal.getCrmCase().getAssignedEmployee() != null
                        ? proposal.getCrmCase().getAssignedEmployee()
                        : employee
        );
        followUp.setScheduledAt(nextFollowUpAt);
        followUp.setStatus(CrmFollowUpStatus.SCHEDULED);
        followUp.setChannel(CrmFollowUpChannel.PHONE);
        followUp.setPurpose(
                !isBlank(note)
                        ? note
                        : "Pipeline follow-up"
        );

        crmFollowUpRepository.save(followUp);

        proposal.getCrmCase().setNextFollowUpAt(nextFollowUpAt);
    }

    @Transactional
    public RishtaPipelineNoteResponse addNote(
            UUID proposalId,
            AddRishtaPipelineNoteRequest request
    ) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        permissionService.assertCanUpdateProposal(proposal);

        RishtaPipelineNote saved = createPipelineNote(
                proposal,
                request.getNote()
        );

        createSimplePipelineTimelineEvent(
                proposal,
                CrmTimelineEventType.PIPELINE_NOTE_ADDED,
                "Pipeline note added",
                saved.getNote()
        );

        auditLogService.record(
                AuditAction.PIPELINE_NOTE_ADDED,
                AuditEntityType.PROPOSAL,
                proposal.getId(),
                "Pipeline note added"
        );


        return RishtaPipelineNoteResponse.builder()
                .noteId(saved.getId())
                .proposalId(proposal.getId())
                .note(saved.getNote())
                .createdByName(saved.getCreatedByName())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional
    public PipelineFollowUpResponse createFollowUp(
            UUID proposalId,
            CreatePipelineFollowUpRequest request
    ) {

        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        permissionService.assertCanUpdateProposal(proposal);

        if (proposal.getCrmCase() == null) {
            throw new IllegalStateException("Proposal has no CRM case");
        }

        EmployeeAccount employee =
                permissionService.getCurrentEmployee();

        CrmFollowUp followUp = new CrmFollowUp();

        followUp.setCrmCase(proposal.getCrmCase());

        followUp.setProposal(proposal);

        followUp.setAssignedEmployee(
                proposal.getCrmCase().getAssignedEmployee() != null
                        ? proposal.getCrmCase().getAssignedEmployee()
                        : employee
        );

        followUp.setScheduledAt(request.getScheduledAt());

        followUp.setStatus(CrmFollowUpStatus.SCHEDULED);

        followUp.setChannel(request.getChannel());

        followUp.setPurpose(
                !isBlank(request.getPurpose())
                        ? request.getPurpose().trim()
                        : "Pipeline follow-up"
        );

        CrmFollowUp saved =
                crmFollowUpRepository.save(followUp);

        proposal.getCrmCase()
                .setNextFollowUpAt(request.getScheduledAt());

        createSimplePipelineTimelineEvent(
                proposal,
                CrmTimelineEventType.PIPELINE_FOLLOW_UP_SCHEDULED,
                "Pipeline follow-up scheduled",
                "Follow-up scheduled at " + request.getScheduledAt()
        );

        auditLogService.record(
                AuditAction.PIPELINE_FOLLOW_UP_SCHEDULED,
                AuditEntityType.PROPOSAL,
                proposal.getId(),
                "Pipeline follow-up scheduled"
        );

        return PipelineFollowUpResponse.builder()
                .followUpId(saved.getId())
                .proposalId(proposal.getId())
                .crmCaseId(proposal.getCrmCase().getId())
                .scheduledAt(saved.getScheduledAt())
                .channel(saved.getChannel())
                .status(saved.getStatus())
                .purpose(saved.getPurpose())
                .assignedEmployeeId(
                        saved.getAssignedEmployee() != null
                                ? saved.getAssignedEmployee().getId()
                                : null
                )
                .assignedEmployeeName(
                        saved.getAssignedEmployee() != null
                                ? saved.getAssignedEmployee().getFullName()
                                : null
                )
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public RishtaPipelineDetailResponse getPipelineDetail(UUID proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new EntityNotFoundException("Proposal not found"));

        permissionService.assertCanViewProposal(proposal);

        RishtaPipelineItemResponse pipeline = mapProposal(proposal);

        SubscriptionResponse subscription = null;

        if (proposal.getFromProfile() != null
                && proposal.getFromProfile().getUserAccount() != null) {
            subscription = revenueService
                    .getFamilySubscription(proposal.getFromProfile().getUserAccount().getId())
                    .getCurrentSubscription();
        }

        return RishtaPipelineDetailResponse.builder()
                .pipeline(pipeline)
                .proposal(toProposalResponse(proposal))
                .statusHistory(getStatusHistory(proposalId))
                .timeline(getTimeline(proposal))
                .subscription(subscription)
                .notes(getProposalNotes(proposalId))
                .followUps(getProposalFollowUps(proposalId))
                .build();
    }

    private List<RishtaPipelineNoteResponse> getProposalNotes(UUID proposalId) {
        return noteRepository.findByProposalIdOrderByCreatedAtDesc(proposalId)
                .stream()
                .map(note -> RishtaPipelineNoteResponse.builder()
                        .noteId(note.getId())
                        .proposalId(note.getProposal().getId())
                        .note(note.getNote())
                        .createdByName(note.getCreatedByName())
                        .createdAt(note.getCreatedAt())
                        .build())
                .toList();
    }

    private List<PipelineFollowUpResponse> getProposalFollowUps(UUID proposalId) {
        return crmFollowUpRepository.findByProposal_IdOrderByScheduledAtDesc(proposalId)
                .stream()
                .map(followUp -> PipelineFollowUpResponse.builder()
                        .followUpId(followUp.getId())
                        .proposalId(proposalId)
                        .crmCaseId(followUp.getCrmCase() != null
                                ? followUp.getCrmCase().getId()
                                : null)
                        .scheduledAt(followUp.getScheduledAt())
                        .channel(followUp.getChannel())
                        .status(followUp.getStatus())
                        .purpose(followUp.getPurpose())
                        .assignedEmployeeId(followUp.getAssignedEmployee() != null
                                ? followUp.getAssignedEmployee().getId()
                                : null)
                        .assignedEmployeeName(followUp.getAssignedEmployee() != null
                                ? followUp.getAssignedEmployee().getFullName()
                                : null)
                        .createdAt(followUp.getCreatedAt())
                        .build())
                .toList();
    }

    private ProposalResponse toProposalResponse(Proposal proposal) {
        return ProposalResponse.builder()
                .proposalId(proposal.getId())
                .fromProfileId(proposal.getFromProfile() != null
                        ? proposal.getFromProfile().getId()
                        : null)
                .toProfileId(proposal.getToProfile() != null
                        ? proposal.getToProfile().getId()
                        : null)
                .crmCaseId(proposal.getCrmCase() != null
                        ? proposal.getCrmCase().getId()
                        : null)
                .status(proposal.getStatus())
                .dispatchChannel(proposal.getDispatchChannel())
                .note(proposal.getNote())
                .matchScore(proposal.getMatchScore())
                .shareProfilePhoto(proposal.isShareProfilePhoto())
                .dispatchedByName(proposal.getDispatchedByName())
                .dispatchedAt(proposal.getDispatchedAt())
                .lastUpdatedByName(proposal.getLastUpdatedByName())
                .lastUpdatedAt(proposal.getUpdatedAt())
                .build();
    }

    private List<ProposalStatusHistoryResponse> getStatusHistory(UUID proposalId) {
        return proposalStatusHistoryRepository
                .findByProposal_IdOrderByCreatedAtDesc(proposalId)
                .stream()
                .map(history -> ProposalStatusHistoryResponse.builder()
                        .status(history.getStatus())
                        .note(history.getNote())
                        .actorName(history.getActorName())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }


    private List<PipelineTimelineResponse> getTimeline(Proposal proposal) {

        if (proposal.getCrmCase() == null) {
            return List.of();
        }

        return crmTimelineRepository
                .findByCrmCase_IdOrderByCreatedAtDesc(
                        proposal.getCrmCase().getId()
                )
                .stream()
                .map(timeline -> PipelineTimelineResponse.builder()
                        .timelineId(timeline.getId())
                        .eventType(
                                timeline.getEventType() != null
                                        ? timeline.getEventType().name()
                                        : null
                        )
                        .title(timeline.getTitle())
                        .description(timeline.getDescription())
                        .actorName(timeline.getActorName())
                        .createdAt(timeline.getCreatedAt())
                        .build())
                .toList();
    }

    private void createProposalStatusHistory(
            Proposal proposal,
            ProposalStatus status,
            String note
    ) {
        EmployeeAccount employee = permissionService.getCurrentEmployee();

        ProposalStatusHistory history = new ProposalStatusHistory();
        history.setProposal(proposal);
        history.setStatus(status);
        history.setNote(!isBlank(note) ? note.trim() : "Pipeline status changed");
        history.setActorEmployee(employee);
        history.setActorName(employee != null ? employee.getFullName() : "System");

        proposalStatusHistoryRepository.save(history);
    }

    private void createPipelineTimelineEvent(
            Proposal proposal,
            ProposalStatus oldStatus,
            ProposalStatus newStatus,
            String note
    ) {
        if (proposal.getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        RishtaPipelineStage stage = stageResolver.resolve(newStatus);

        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(proposal.getCrmCase());
        timeline.setEventType(resolveTimelineEventType(newStatus));
        timeline.setTitle("Pipeline stage changed");
        timeline.setDescription(
                "Pipeline changed from "
                        + (oldStatus != null ? oldStatus.name() : "NONE")
                        + " to "
                        + newStatus.name()
                        + (!isBlank(note) ? ". Note: " + note.trim() : "")
        );
        timeline.setActorName(employee != null ? employee.getFullName() : "System");
        timeline.setOldValue(oldStatus != null ? oldStatus.name() : null);
        timeline.setNewValue(newStatus.name());

        crmCaseTimelineRepository.save(timeline);
    }


    private CrmTimelineEventType resolveTimelineEventType(ProposalStatus status) {
        if (status == ProposalStatus.ACCEPTED) {
            return CrmTimelineEventType.PIPELINE_ACCEPTED;
        }

        if (status == ProposalStatus.REJECTED || status == ProposalStatus.NOT_INTERESTED) {
            return CrmTimelineEventType.PIPELINE_REJECTED;
        }

        if (status == ProposalStatus.CANCELLED || status == ProposalStatus.EXPIRED) {
            return CrmTimelineEventType.PIPELINE_CLOSED;
        }

        return CrmTimelineEventType.PIPELINE_STAGE_CHANGED;
    }

    private void createPipelineAuditEvent(
            Proposal proposal,
            ProposalStatus oldStatus,
            ProposalStatus newStatus
    ) {
        AuditAction action = resolveAuditAction(newStatus);

        auditLogService.record(
                action,
                AuditEntityType.PROPOSAL,
                proposal.getId(),
                "Pipeline status changed from "
                        + (oldStatus != null ? oldStatus.name() : "NONE")
                        + " to "
                        + newStatus.name()
        );
    }

    private AuditAction resolveAuditAction(ProposalStatus status) {
        if (status == ProposalStatus.ACCEPTED) {
            return AuditAction.PIPELINE_ACCEPTED;
        }

        if (status == ProposalStatus.REJECTED || status == ProposalStatus.NOT_INTERESTED) {
            return AuditAction.PIPELINE_REJECTED;
        }

        if (status == ProposalStatus.CANCELLED || status == ProposalStatus.EXPIRED) {
            return AuditAction.PIPELINE_CLOSED;
        }

        return AuditAction.PIPELINE_STAGE_CHANGED;
    }

    private void createSimplePipelineTimelineEvent(
            Proposal proposal,
            CrmTimelineEventType eventType,
            String title,
            String description
    ) {
        if (proposal.getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(proposal.getCrmCase());
        timeline.setEventType(eventType);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setActorName(employee != null ? employee.getFullName() : "System");

        crmCaseTimelineRepository.save(timeline);
    }



}