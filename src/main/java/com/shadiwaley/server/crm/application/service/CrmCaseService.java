package com.shadiwaley.server.crm.application.service;

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

        return toResponse(crmCaseRepository.save(crmCase));
    }

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

        EmployeeAccount employee = employeeAccountRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        crmCase.setAssignedEmployee(employee);
        crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);

        addSystemNote(crmCase, "Case assigned to " + employee.getFullName());

        return toResponse(crmCaseRepository.save(crmCase));
    }

    @Transactional
    public CrmCaseResponse updateStatus(UUID caseId, UpdateCrmCaseStatusRequest request) {
        CrmCase crmCase = getCase(caseId);

        crmCase.setStatus(request.getStatus());
        crmCase.setLastOutcome(request.getOutcome());

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

        return toNoteResponse(crmCaseNoteRepository.save(note));
    }

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

        return toFollowUpResponse(crmFollowUpRepository.save(followUp));
    }

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
}