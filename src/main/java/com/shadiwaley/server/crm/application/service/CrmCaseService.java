package com.shadiwaley.server.crm.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.crm.domain.*;
import com.shadiwaley.server.crm.dto.request.*;
import com.shadiwaley.server.crm.dto.response.*;
import com.shadiwaley.server.crm.infrastructure.entity.*;
import com.shadiwaley.server.crm.infrastructure.repository.*;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmCaseService {

    private final CrmCaseRepository crmCaseRepository;
    private final CrmCaseNoteRepository crmCaseNoteRepository;
    private final CrmFollowUpRepository crmFollowUpRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final EmployeeAccountRepository employeeAccountRepository;
    private final AuditLogService auditLogService;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;

    @Transactional
    public CrmCaseResponse createCase(CreateCrmCaseRequest request) {
        UserAccount user = userAccountRepository.findById(request.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        EmployeeAccount assignedEmployee = null;
        if (request.getAssignedEmployeeId() != null) {
            assignedEmployee = employeeAccountRepository.findById(request.getAssignedEmployeeId())
                    .orElseThrow(() -> new EntityNotFoundException("Assigned employee not found"));
        }

        EmployeeAccount creator = getCurrentEmployeeOrNull();

        CrmCase crmCase = new CrmCase();
        crmCase.setUserAccount(user);
        crmCase.setUserProfile(profile);
        crmCase.setAssignedEmployee(assignedEmployee);
        crmCase.setCaseType(request.getCaseType());
        crmCase.setPriority(request.getPriority() == null ? CrmCasePriority.MEDIUM : request.getPriority());
        crmCase.setStatus(CrmCaseStatus.OPEN);
        crmCase.setSource(request.getSource());
        crmCase.setSummary(request.getSummary());
        crmCase.setCreatedByEmployee(creator);

        CrmCase saved = crmCaseRepository.save(crmCase);
        addTimeline(
                saved,
                CrmTimelineEventType.CASE_CREATED,
                "Case created",
                "CRM case was created.",
                null,
                saved.getStatus().name()
        );

        auditLogService.record(
                AuditAction.CRM_CASE_CREATED,
                AuditEntityType.CRM_CASE,
                saved.getId(),
                "CRM case created"
        );

        return toResponse(saved);    }

    @Transactional(readOnly = true)
    public CrmCasePageResponse getCases(
            int page,
            int size,
            CrmCaseStatus status,
            CrmCasePriority priority,
            UUID assignedEmployeeId,
            String search
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<CrmCase> result = crmCaseRepository.findAll(
                specification(status, priority, assignedEmployeeId, search),
                pageable
        );

        return CrmCasePageResponse.builder()
                .cases(result.getContent().stream().map(this::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public CrmCaseDetailResponse getCaseDetail(UUID caseId) {
        CrmCase crmCase = getCase(caseId);

        return CrmCaseDetailResponse.builder()
                .crmCase(toResponse(crmCase))
                .notes(crmCaseNoteRepository.findByCrmCaseIdOrderByCreatedAtDesc(caseId)
                        .stream()
                        .map(this::toNoteResponse)
                        .toList())
                .followUps(crmFollowUpRepository.findAll()
                        .stream()
                        .filter(followUp -> followUp.getCrmCase().getId().equals(caseId))
                        .map(this::toFollowUpResponse)
                        .toList())
                .build();
    }

    @Transactional
    public CrmCaseResponse assign(UUID caseId, AssignCrmCaseRequest request) {
        CrmCase crmCase = getCase(caseId);
        String oldAssignee = crmCase.getAssignedEmployee() != null
                ? crmCase.getAssignedEmployee().getFullName()
                : null;

        EmployeeAccount employee = employeeAccountRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        crmCase.setAssignedEmployee(employee);
        crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);

        addTimeline(
                crmCase,
                CrmTimelineEventType.CRM_ASSIGNED,
                "CRM assigned",
                "Case assigned to " + employee.getFullName(),
                oldAssignee,
                employee.getFullName()
        );

        addSystemNote(crmCase, "Case assigned to " + employee.getFullName());
        auditLogService.record(
                AuditAction.CRM_CASE_ASSIGNED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "CRM case assigned to " + employee.getFullName()
        );

        return toResponse(crmCaseRepository.save(crmCase));
    }

    @Transactional
    public CrmCaseResponse updateStatus(UUID caseId, UpdateCrmCaseStatusRequest request) {
        CrmCase crmCase = getCase(caseId);
        String oldStatus = crmCase.getStatus() != null ? crmCase.getStatus().name() : null;

        crmCase.setStatus(request.getStatus());
        crmCase.setLastOutcome(request.getOutcome());

        addTimeline(
                crmCase,
                request.getStatus() == CrmCaseStatus.CLOSED || request.getStatus() == CrmCaseStatus.RESOLVED
                        ? CrmTimelineEventType.CASE_CLOSED
                        : CrmTimelineEventType.CASE_UPDATED,
                "Status changed",
                "Status changed to " + request.getStatus(),
                oldStatus,
                request.getStatus().name()
        );


        auditLogService.record(
                AuditAction.CRM_CASE_STATUS_CHANGED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "CRM case status changed to " + request.getStatus()
        );

        if (request.getStatus() == CrmCaseStatus.CLOSED || request.getStatus() == CrmCaseStatus.RESOLVED) {
            crmCase.setClosedAt(Instant.now());
        }

        if (request.getOutcome() != null && !request.getOutcome().isBlank()) {
            addSystemNote(crmCase, "Status changed to " + request.getStatus() + ". Outcome: " + request.getOutcome());
        } else {
            addSystemNote(crmCase, "Status changed to " + request.getStatus());
        }

        return toResponse(crmCaseRepository.save(crmCase));
    }

    @Transactional
    public CrmNoteResponse addNote(UUID caseId, CreateCrmNoteRequest request) {
        CrmCase crmCase = getCase(caseId);

        CrmCaseNote note = new CrmCaseNote();
        note.setCrmCase(crmCase);
        note.setEmployee(getCurrentEmployeeOrNull());
        note.setNoteType(request.getNoteType());
        note.setNote(request.getNote());

        CrmCaseNote saved = crmCaseNoteRepository.save(note);
        addTimeline(
                crmCase,
                CrmTimelineEventType.NOTE_ADDED,
                "Note added",
                request.getNote(),
                null,
                null
        );

        auditLogService.record(
                AuditAction.CRM_NOTE_ADDED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "CRM note added"
        );

        return toNoteResponse(saved);    }

    @Transactional
    public CrmFollowUpResponse createFollowUp(UUID caseId, CreateFollowUpRequest request) {
        CrmCase crmCase = getCase(caseId);

        CrmFollowUp followUp = new CrmFollowUp();
        followUp.setCrmCase(crmCase);
        followUp.setAssignedEmployee(crmCase.getAssignedEmployee());
        followUp.setScheduledAt(request.getScheduledAt());
        followUp.setChannel(request.getChannel());
        followUp.setPurpose(request.getPurpose());
        followUp.setStatus(CrmFollowUpStatus.SCHEDULED);

        crmCase.setNextFollowUpAt(request.getScheduledAt());
        crmCaseRepository.save(crmCase);

        CrmFollowUp saved = crmFollowUpRepository.save(followUp);

        addTimeline(
                crmCase,
                CrmTimelineEventType.FOLLOW_UP_SCHEDULED,
                "Follow-up scheduled",
                request.getPurpose(),
                null,
                request.getScheduledAt().toString()
        );

        auditLogService.record(
                AuditAction.CRM_FOLLOW_UP_CREATED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "CRM follow-up scheduled"
        );

        return toFollowUpResponse(saved);    }

    private Specification<CrmCase> specification(
            CrmCaseStatus status,
            CrmCasePriority priority,
            UUID assignedEmployeeId,
            String search
    ) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicate = cb.and(predicate, cb.equal(root.get("priority"), priority));
            }

            if (assignedEmployeeId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("assignedEmployee").get("id"), assignedEmployeeId));
            }

            if (search != null && !search.isBlank()) {
                Join<CrmCase, UserAccount> userJoin = root.join("userAccount", JoinType.INNER);
                Join<CrmCase, UserProfile> profileJoin = root.join("userProfile", JoinType.INNER);

                String pattern = "%" + search.toLowerCase() + "%";

                predicate = cb.and(predicate,
                        cb.or(
                                cb.like(cb.lower(userJoin.get("phone")), pattern),
                                cb.like(cb.lower(profileJoin.get("candidateFirstName")), pattern),
                                cb.like(cb.lower(profileJoin.get("displayId")), pattern)
                        )
                );
            }

            return predicate;
        };
    }

    private void addSystemNote(CrmCase crmCase, String noteText) {
        CrmCaseNote note = new CrmCaseNote();
        note.setCrmCase(crmCase);
        note.setEmployee(getCurrentEmployeeOrNull());
        note.setNoteType(CrmNoteType.SYSTEM_NOTE);
        note.setNote(noteText);
        crmCaseNoteRepository.save(note);
    }

    private CrmCase getCase(UUID caseId) {
        return crmCaseRepository.findById(caseId)
                .orElseThrow(() -> new EntityNotFoundException("CRM case not found"));
    }

    private EmployeeAccount getCurrentEmployeeOrNull() {
        try {
            return employeeAccountRepository.findById(AuthUser.getCurrentActorId()).orElse(null);
        } catch (Exception ex) {
            return null;
        }
    }

    private CrmCaseResponse toResponse(CrmCase crmCase) {
        UserAccount user = crmCase.getUserAccount();
        UserProfile profile = crmCase.getUserProfile();
        ParentProfile parent = parentProfileRepository.findByUserAccountId(user.getId()).orElse(null);
        EmployeeAccount assigned = crmCase.getAssignedEmployee();

        return CrmCaseResponse.builder()
                .caseId(crmCase.getId())
                .userId(user.getId())
                .profileId(profile.getId())
                .displayId(profile.getDisplayId())
                .phone(user.getPhone())
                .side(user.getSide())
                .candidateName(profile.getCandidateFirstName())
                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)
                .parentRelation(parent != null && parent.getParentRelation() != null
                        ? parent.getParentRelation().name()
                        : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .completionPct(profile.getCompletionPct())
                .profileStatus(profile.getProfileStatus())
                .assignedEmployeeId(assigned != null ? assigned.getId() : null)
                .assignedEmployeeName(assigned != null ? assigned.getFullName() : null)
                .caseType(crmCase.getCaseType())
                .status(crmCase.getStatus())
                .priority(crmCase.getPriority())
                .source(crmCase.getSource())
                .summary(crmCase.getSummary())
                .lastOutcome(crmCase.getLastOutcome())
                .nextFollowUpAt(crmCase.getNextFollowUpAt())
                .closedAt(crmCase.getClosedAt())
                .stage(crmCase.getStage())
                .createdAt(crmCase.getCreatedAt())
                .updatedAt(crmCase.getUpdatedAt())
                .build();
    }

    private CrmNoteResponse toNoteResponse(CrmCaseNote note) {
        EmployeeAccount employee = note.getEmployee();

        return CrmNoteResponse.builder()
                .noteId(note.getId())
                .employeeId(employee != null ? employee.getId() : null)
                .employeeName(employee != null ? employee.getFullName() : "System")
                .noteType(note.getNoteType())
                .note(note.getNote())
                .createdAt(note.getCreatedAt())
                .build();
    }

    private CrmFollowUpResponse toFollowUpResponse(CrmFollowUp followUp) {
        EmployeeAccount employee = followUp.getAssignedEmployee();

        return CrmFollowUpResponse.builder()
                .followUpId(followUp.getId())
                .assignedEmployeeId(employee != null ? employee.getId() : null)
                .assignedEmployeeName(employee != null ? employee.getFullName() : null)
                .scheduledAt(followUp.getScheduledAt())
                .completedAt(followUp.getCompletedAt())
                .status(followUp.getStatus())
                .channel(followUp.getChannel())
                .purpose(followUp.getPurpose())
                .outcome(followUp.getOutcome())
                .build();
    }

    @Transactional
    public CrmCaseResponse updateStage(UUID caseId, UpdateCrmStageRequest request) {
        CrmCase crmCase = getCase(caseId);

        CrmCaseStage oldStageEnum = crmCase.getStage();
        CrmCaseStatus oldStatusEnum = crmCase.getStatus();

        String oldStage = oldStageEnum != null ? oldStageEnum.name() : null;
        String oldStatus = oldStatusEnum != null ? oldStatusEnum.name() : null;

        boolean wasClosed = isClosedStage(oldStageEnum);
        boolean nowClosed = isClosedStage(request.getStage());

        crmCase.setStage(request.getStage());

        if (nowClosed) {
            crmCase.setStatus(CrmCaseStatus.CLOSED);

            if (crmCase.getClosedAt() == null) {
                crmCase.setClosedAt(Instant.now());
            }

            addTimeline(
                    crmCase,
                    CrmTimelineEventType.CASE_CLOSED,
                    "Case closed",
                    request.getNote() != null && !request.getNote().isBlank()
                            ? request.getNote()
                            : "Case moved to closed stage " + request.getStage(),
                    oldStatus,
                    CrmCaseStatus.CLOSED.name()
            );
        } else if (wasClosed && isActiveStage(request.getStage())) {
            crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            crmCase.setClosedAt(null);

            addTimeline(
                    crmCase,
                    CrmTimelineEventType.CASE_REOPENED,
                    "Case reopened",
                    request.getNote() != null && !request.getNote().isBlank()
                            ? request.getNote()
                            : "Case reopened from closed stage",
                    oldStatus,
                    CrmCaseStatus.IN_PROGRESS.name()
            );
        } else {
            if (crmCase.getStatus() == CrmCaseStatus.OPEN || crmCase.getStatus() == null) {
                crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);
            }
        }

        if (request.getStage() == CrmCaseStage.CONTACTED) {
            crmCase.setLastContactAt(Instant.now());
        }

        addTimeline(
                crmCase,
                CrmTimelineEventType.STAGE_CHANGED,
                "Stage changed",
                "Stage changed to " + request.getStage(),
                oldStage,
                request.getStage().name()
        );

        if (request.getNote() != null && !request.getNote().isBlank()) {
            addSystemNote(crmCase, "Stage changed to " + request.getStage() + ". Note: " + request.getNote());
        } else {
            addSystemNote(crmCase, "Stage changed to " + request.getStage());
        }

        auditLogService.record(
                AuditAction.CRM_CASE_STATUS_CHANGED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "CRM case stage changed to " + request.getStage()
        );

        return toResponse(crmCaseRepository.save(crmCase));
    }


    @Transactional
    public CrmFollowUpResponse completeFollowUp(
            UUID caseId,
            UUID followUpId,
            CompleteFollowUpRequest request
    ) {
        CrmCase crmCase = getCase(caseId);

        CrmFollowUp followUp = crmFollowUpRepository.findById(followUpId)
                .orElseThrow(() -> new EntityNotFoundException("Follow-up not found"));

        if (!followUp.getCrmCase().getId().equals(caseId)) {
            throw new IllegalArgumentException("Follow-up does not belong to this case");
        }

        followUp.setCompletedAt(Instant.now());
        followUp.setStatus(CrmFollowUpStatus.COMPLETED);
        followUp.setOutcome(request.getNote());

        crmCase.setLastContactAt(Instant.now());
        crmCase.setLastOutcome(request.getNote());

        if (request.getNote() != null && !request.getNote().isBlank()) {
            addSystemNote(crmCase, "Follow-up completed. Outcome: " + request.getNote());
        } else {
            addSystemNote(crmCase, "Follow-up completed");
        }

        crmCaseRepository.save(crmCase);

        addTimeline(
                crmCase,
                CrmTimelineEventType.FOLLOW_UP_COMPLETED,
                "Follow-up completed",
                request.getNote(),
                null,
                followUp.getCompletedAt().toString()
        );

        return toFollowUpResponse(crmFollowUpRepository.save(followUp));
    }

    @Transactional(readOnly = true)
    public List<CrmTimelineResponse> getTimeline(UUID caseId) {
        CrmCase crmCase = getCase(caseId);

        return crmCaseTimelineRepository
                .findByCrmCaseIdOrderByCreatedAtDesc(crmCase.getId())
                .stream()
                .map(this::toTimelineResponse)
                .toList();
    }

    private void addTimeline(
            CrmCase crmCase,
            CrmTimelineEventType eventType,
            String title,
            String description,
            String oldValue,
            String newValue
    ) {
        EmployeeAccount actor = getCurrentEmployeeOrNull();

        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(crmCase);
        timeline.setEventType(eventType);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setActorEmployee(actor);
        timeline.setActorName(actor != null ? actor.getFullName() : "System");
        timeline.setOldValue(oldValue);
        timeline.setNewValue(newValue);

        crmCaseTimelineRepository.save(timeline);
    }

    private CrmTimelineResponse toTimelineResponse(CrmCaseTimeline timeline) {
        EmployeeAccount actor = timeline.getActorEmployee();

        return CrmTimelineResponse.builder()
                .eventId(timeline.getId())
                .eventType(timeline.getEventType())
                .title(timeline.getTitle())
                .description(timeline.getDescription())
                .actorEmployeeId(actor != null ? actor.getId() : null)
                .actorName(timeline.getActorName())
                .oldValue(timeline.getOldValue())
                .newValue(timeline.getNewValue())
                .metadata(timeline.getMetadata())
                .createdAt(timeline.getCreatedAt())
                .build();
    }

    private boolean isClosedStage(CrmCaseStage stage) {
        return stage == CrmCaseStage.CLOSED_SUCCESS
                || stage == CrmCaseStage.CLOSED_NOT_INTERESTED
                || stage == CrmCaseStage.CLOSED_UNREACHABLE;
    }

    private boolean isActiveStage(CrmCaseStage stage) {
        return stage == CrmCaseStage.NEW
                || stage == CrmCaseStage.CONTACT_PENDING
                || stage == CrmCaseStage.CONTACTED
                || stage == CrmCaseStage.PROFILE_DISCUSSION
                || stage == CrmCaseStage.MATCH_SUGGESTED
                || stage == CrmCaseStage.FAMILY_INTERESTED
                || stage == CrmCaseStage.FOLLOW_UP;
    }


}