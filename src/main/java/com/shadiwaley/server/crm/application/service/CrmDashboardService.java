package com.shadiwaley.server.crm.application.service;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStage;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.crm.dto.response.*;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseTimelineRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmFollowUpRepository;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final CrmCaseRepository crmCaseRepository;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmployeeAccountRepository employeeAccountRepository;
    private final CrmFollowUpRepository crmFollowUpRepository;

    @Transactional(readOnly = true)
    public CrmDashboardResponse getDashboard() {

        EmployeeAccount currentEmployee = getCurrentEmployeeOrNull();

        boolean isCrmAgent = currentEmployee != null
                && currentEmployee.getRole() == EmployeeRole.CRM_AGENT;

        Instant now = Instant.now();

        Instant startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant endOfDay = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant monthStart = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        UUID employeeId = isCrmAgent ? currentEmployee.getId() : null;

        return CrmDashboardResponse.builder()
                .summary(buildSummary(now, startOfDay, endOfDay, monthStart, employeeId))

                .todayFollowUps(
                        getTodayFollowUps(startOfDay, endOfDay, employeeId)
                                .stream()
                                .map(this::toFollowUpResponse)
                                .toList()
                )

                .overdueFollowUps(
                        getOverdueFollowUps(now, employeeId)
                                .stream()
                                .map(this::toFollowUpResponse)
                                .toList()
                )

                .unassignedCases(
                        isCrmAgent
                                ? List.of()
                                : crmCaseRepository
                                .findTop10ByAssignedEmployeeIsNullOrderByCreatedAtDesc()
                                .stream()
                                .map(this::toUnassignedResponse)
                                .toList()
                )

                .recentActivity(
                        getRecentActivity(employeeId)
                                .stream()
                                .map(this::toTimelineResponse)
                                .toList()
                )

                .employeeWorkload(
                        isCrmAgent ? List.of() : buildEmployeeWorkload(monthStart)
                )

                .build();
    }

    private CrmDashboardSummaryResponse buildSummary(
            Instant now,
            Instant startOfDay,
            Instant endOfDay,
            Instant monthStart,
            UUID employeeId
    ) {
        boolean assignedOnly = employeeId != null;

        return CrmDashboardSummaryResponse.builder()

                .openCases(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndStatus(employeeId, CrmCaseStatus.OPEN)
                                : crmCaseRepository.countByStatus(CrmCaseStatus.OPEN)
                )

                .inProgressCases(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndStatus(employeeId, CrmCaseStatus.IN_PROGRESS)
                                : crmCaseRepository.countByStatus(CrmCaseStatus.IN_PROGRESS)
                )

                .waitingCases(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndStage(employeeId, CrmCaseStage.FOLLOW_UP)
                                : crmCaseRepository.countByStage(CrmCaseStage.FOLLOW_UP)
                )

                .urgentCases(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndPriority(employeeId, CrmCasePriority.URGENT)
                                : crmCaseRepository.countByPriority(CrmCasePriority.URGENT)
                )

                .highPriorityCases(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndPriority(employeeId, CrmCasePriority.HIGH)
                                : crmCaseRepository.countByPriority(CrmCasePriority.HIGH)
                )

                .closedThisMonth(
                        assignedOnly
                                ? crmCaseRepository.countByAssignedEmployeeIdAndClosedAtBetween(employeeId, monthStart, now)
                                : crmCaseRepository.countByClosedAtBetween(monthStart, now)
                )

                .unassignedCases(
                        assignedOnly ? 0 : crmCaseRepository.countByAssignedEmployeeIsNull()
                )

                .followUpsDueToday(
                        getTodayFollowUps(startOfDay, endOfDay, employeeId).size()
                )

                .overdueFollowUps(
                        getOverdueFollowUps(now, employeeId).size()
                )

                .pendingVerification(0)

                .build();
    }

    private CrmDashboardFollowUpResponse toFollowUpResponse(CrmFollowUp followUp) {

        CrmCase crmCase = followUp.getCrmCase();

        ParentProfile parent = parentProfileRepository
                .findTopByUserAccountIdOrderByCreatedAtDesc(
                        crmCase.getUserAccount().getId()
                )
                .orElse(null);

        EmployeeAccount assignedEmployee = crmCase.getAssignedEmployee();

        return CrmDashboardFollowUpResponse.builder()
                .followUpId(followUp.getId())
                .caseId(crmCase.getId())
                .candidateName(
                        crmCase.getUserProfile() != null
                                ? crmCase.getUserProfile().getCandidateFirstName()
                                : null
                )
                .parentName(parent != null ? parent.getParentName() : null)
                .phone(parent != null ? parent.getParentPhone() : crmCase.getUserAccount().getPhone())
                .assignedEmployeeName(
                        assignedEmployee != null ? assignedEmployee.getFullName() : null
                )
                .priority(crmCase.getPriority())
                .stage(crmCase.getStage())
                .nextFollowUpAt(followUp.getScheduledAt())
                .channel(followUp.getChannel() != null ? followUp.getChannel().name() : null)
                .purpose(followUp.getPurpose())
                .completed(followUp.getCompletedAt() != null)
                .build();
    }

    private CrmDashboardUnassignedCaseResponse toUnassignedResponse(CrmCase crmCase) {

        ParentProfile parent = parentProfileRepository
                .findTopByUserAccountIdOrderByCreatedAtDesc(
                        crmCase.getUserAccount().getId()
                )
                .orElse(null);

        return CrmDashboardUnassignedCaseResponse.builder()
                .caseId(crmCase.getId())
                .candidateName(
                        crmCase.getUserProfile() != null
                                ? crmCase.getUserProfile().getCandidateFirstName()
                                : null
                )
                .parentName(parent != null ? parent.getParentName() : null)
                .phone(parent != null ? parent.getParentPhone() : null)
                .district(parent != null ? parent.getDistrict() : null)
                .priority(crmCase.getPriority())
                .createdAt(crmCase.getCreatedAt())
                .build();
    }

    private CrmTimelineResponse toTimelineResponse(CrmCaseTimeline timeline) {

        return CrmTimelineResponse.builder()
                .eventId(timeline.getId())
                .caseId(timeline.getCrmCase().getId())
                .eventType(timeline.getEventType())
                .title(timeline.getTitle())
                .description(timeline.getDescription())
                .actorName(timeline.getActorName())
                .oldValue(timeline.getOldValue())
                .newValue(timeline.getNewValue())
                .createdAt(timeline.getCreatedAt())
                .build();
    }

    private List<CrmEmployeeWorkloadResponse> buildEmployeeWorkload(
            Instant monthStart
    ) {

        Instant now = Instant.now();

        Instant startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant endOfDay = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        return employeeAccountRepository
                .findAll()
                .stream()
                .map(employee -> {

                    long openCases =
                            crmCaseRepository.countByAssignedEmployeeIdAndStatus(
                                    employee.getId(),
                                    CrmCaseStatus.OPEN
                            );

                    long closedThisMonth =
                            crmCaseRepository
                                    .countByAssignedEmployeeIdAndClosedAtBetween(
                                            employee.getId(),
                                            monthStart,
                                            now
                                    );

                    long dueToday =
                            crmCaseRepository
                                    .findTop10ByNextFollowUpAtBetweenOrderByNextFollowUpAtAsc(
                                            startOfDay,
                                            endOfDay
                                    )
                                    .stream()
                                    .filter(c ->
                                            c.getAssignedEmployee() != null
                                                    && c.getAssignedEmployee()
                                                    .getId()
                                                    .equals(employee.getId())
                                    )
                                    .count();

                    return CrmEmployeeWorkloadResponse.builder()
                            .employeeId(employee.getId())
                            .employeeName(employee.getFullName())
                            .openCases(openCases)
                            .dueToday(dueToday)
                            .closedThisMonth(closedThisMonth)
                            .build();
                })
                .toList();
    }

    private List<CrmFollowUp> getTodayFollowUps(
            Instant startOfDay,
            Instant endOfDay,
            UUID employeeId
    ) {
        if (employeeId != null) {
            return crmFollowUpRepository
                    .findTop10ByAssignedEmployeeIdAndStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
                            employeeId,
                            CrmFollowUpStatus.SCHEDULED,
                            startOfDay,
                            endOfDay
                    );
        }

        return crmFollowUpRepository
                .findTop10ByStatusAndScheduledAtBetweenOrderByScheduledAtAsc(
                        CrmFollowUpStatus.SCHEDULED,
                        startOfDay,
                        endOfDay
                );
    }

    private List<CrmFollowUp> getOverdueFollowUps(
            Instant now,
            UUID employeeId
    ) {
        if (employeeId != null) {
            return crmFollowUpRepository
                    .findTop10ByAssignedEmployeeIdAndStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                            employeeId,
                            CrmFollowUpStatus.SCHEDULED,
                            now
                    );
        }

        return crmFollowUpRepository
                .findTop10ByStatusAndScheduledAtBeforeOrderByScheduledAtAsc(
                        CrmFollowUpStatus.SCHEDULED,
                        now
                );
    }

    private List<CrmCaseTimeline> getRecentActivity(UUID employeeId) {
        if (employeeId != null) {
            return crmCaseTimelineRepository
                    .findTop20ByCrmCaseAssignedEmployeeIdOrderByCreatedAtDesc(employeeId);
        }

        return crmCaseTimelineRepository.findTop20ByOrderByCreatedAtDesc();
    }

    private EmployeeAccount getCurrentEmployeeOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return null;
        }

        String principal = authentication.getName();

        try {
            UUID actorId = AuthUser.getCurrentActorId();
            Optional<EmployeeAccount> byId = employeeAccountRepository.findById(actorId);
            if (byId.isPresent()) {
                return byId.get();
            }
        } catch (Exception ignored) {
        }

        try {
            UUID principalId = UUID.fromString(principal);
            Optional<EmployeeAccount> byPrincipalId = employeeAccountRepository.findById(principalId);
            if (byPrincipalId.isPresent()) {
                return byPrincipalId.get();
            }
        } catch (Exception ignored) {
        }

        if (principal != null && principal.contains("@")) {
            return employeeAccountRepository
                    .findByEmailIgnoreCase(principal.trim().toLowerCase())
                    .orElse(null);
        }

        return null;
    }




}
