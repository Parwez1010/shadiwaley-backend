package com.shadiwaley.server.crm.application.service;

import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStage;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.dto.response.*;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseTimelineRepository;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmDashboardService {

    private final CrmCaseRepository crmCaseRepository;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserProfileRepository userProfileRepository;
    private final EmployeeAccountRepository employeeAccountRepository;

    @Transactional(readOnly = true)
    public CrmDashboardResponse getDashboard() {

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

        return CrmDashboardResponse.builder()

                .summary(buildSummary(now, startOfDay, endOfDay, monthStart))

                .todayFollowUps(
                        crmCaseRepository
                                .findTop10ByNextFollowUpAtBetweenOrderByNextFollowUpAtAsc(
                                        startOfDay,
                                        endOfDay
                                )
                                .stream()
                                .map(this::toFollowUpResponse)
                                .toList()
                )

                .overdueFollowUps(
                        crmCaseRepository
                                .findTop10ByNextFollowUpAtBeforeOrderByNextFollowUpAtAsc(now)
                                .stream()
                                .map(this::toFollowUpResponse)
                                .toList()
                )

                .unassignedCases(
                        crmCaseRepository
                                .findTop10ByAssignedEmployeeIsNullOrderByCreatedAtDesc()
                                .stream()
                                .map(this::toUnassignedResponse)
                                .toList()
                )

                .recentActivity(
                        crmCaseTimelineRepository
                                .findTop20ByOrderByCreatedAtDesc()
                                .stream()
                                .map(this::toTimelineResponse)
                                .toList()
                )

                .employeeWorkload(buildEmployeeWorkload(monthStart))

                .build();
    }

    private CrmDashboardSummaryResponse buildSummary(
            Instant now,
            Instant startOfDay,
            Instant endOfDay,
            Instant monthStart
    ) {

        return CrmDashboardSummaryResponse.builder()

                .openCases(
                        crmCaseRepository.countByStatus(CrmCaseStatus.OPEN)
                )

                .inProgressCases(
                        crmCaseRepository.countByStatus(CrmCaseStatus.IN_PROGRESS)
                )

                .waitingCases(
                        crmCaseRepository.countByStage(CrmCaseStage.FOLLOW_UP)
                )

                .urgentCases(
                        crmCaseRepository.countByPriority(CrmCasePriority.URGENT)
                )

                .highPriorityCases(
                        crmCaseRepository.countByPriority(CrmCasePriority.HIGH)
                )

                .closedThisMonth(
                        crmCaseRepository.countByClosedAtBetween(
                                monthStart,
                                now
                        )
                )

                .unassignedCases(
                        crmCaseRepository.countByAssignedEmployeeIsNull()
                )

                .followUpsDueToday(
                        crmCaseRepository
                                .findTop10ByNextFollowUpAtBetweenOrderByNextFollowUpAtAsc(
                                        startOfDay,
                                        endOfDay
                                )
                                .size()
                )

                .overdueFollowUps(
                        crmCaseRepository
                                .findTop10ByNextFollowUpAtBeforeOrderByNextFollowUpAtAsc(now)
                                .size()
                )

                .pendingVerification(0)

                .build();
    }

    private CrmDashboardFollowUpResponse toFollowUpResponse(CrmCase crmCase) {

        ParentProfile parent = parentProfileRepository
                .findTopByUserAccountIdOrderByCreatedAtDesc(
                        crmCase.getUserAccount().getId()
                )
                .orElse(null);

        return CrmDashboardFollowUpResponse.builder()
                .caseId(crmCase.getId())
                .candidateName(
                        crmCase.getUserProfile() != null
                                ? crmCase.getUserProfile().getCandidateFirstName()
                                : null
                )
                .parentName(parent != null ? parent.getParentName() : null)
                .phone(parent != null ? parent.getParentPhone() : null)
                .assignedEmployeeName(
                        crmCase.getAssignedEmployee() != null
                                ? crmCase.getAssignedEmployee().getFullName()
                                : null
                )
                .priority(crmCase.getPriority())
                .stage(crmCase.getStage())
                .nextFollowUpAt(crmCase.getNextFollowUpAt())
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


}
