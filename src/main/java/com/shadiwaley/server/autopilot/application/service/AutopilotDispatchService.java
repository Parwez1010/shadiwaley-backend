package com.shadiwaley.server.autopilot.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.autopilot.domain.*;
import com.shadiwaley.server.autopilot.dto.request.*;
import com.shadiwaley.server.autopilot.dto.response.*;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchBatch;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchDraft;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchItem;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotDispatchQueue;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotDispatchBatchRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotDispatchDraftRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotDispatchItemRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotDispatchQueueRepository;
import com.shadiwaley.server.crm.domain.CrmFollowUpChannel;
import com.shadiwaley.server.crm.domain.CrmFollowUpStatus;
import com.shadiwaley.server.crm.domain.CrmTimelineEventType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCaseTimeline;
import com.shadiwaley.server.crm.infrastructure.entity.CrmFollowUp;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseTimelineRepository;
import com.shadiwaley.server.crm.infrastructure.repository.CrmFollowUpRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalDispatchChannel;
import com.shadiwaley.server.proposal.domain.ProposalSourceType;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import com.shadiwaley.server.rishtapipeline.application.service.RishtaPipelineStageResolver;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AutopilotDispatchService {

    private final AutopilotDispatchQueueRepository queueRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final AutopilotPermissionService permissionService;

    private final UserProfileRepository userProfileRepository;
    private final ProposalRepository proposalRepository;
    private final AutopilotDispatchItemRepository dispatchItemRepository;
    private final AutopilotDispatchDraftRepository draftRepository;

    private final AutopilotDispatchBatchRepository batchRepository;
    private final RishtaPipelineStageResolver pipelineStageResolver;
    private final CrmFollowUpRepository crmFollowUpRepository;

    private final CrmCaseRepository crmCaseRepository;
    private final RevenueService revenueService;

    private final AuditLogService auditLogService;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;

    @Transactional(readOnly = true)
    public AutopilotQueuePageResponse getQueue(
            int page,
            int size,
            String search,
            AutopilotQueueStatus status,
            UUID assignedEmployeeId,
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
        UUID effectiveEmployeeId =
                permissionService.effectiveAssignedEmployeeId(assignedEmployeeId);

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        Page<AutopilotDispatchQueue> result =
                queueRepository.searchQueue(
                        blankToNull(search),
                        status != null ? status.name() : null,
                        effectiveEmployeeId,
                        blankToNull(district),
                        blankToNull(side),
                        blankToNull(planCode),
                        blankToNull(paymentStatus),
                        blankToNull(subscriptionStatus),
                        from,
                        to,
                        pageable
                );

        var items = result.getContent()
                .stream()
                .map(this::toQueueItem)
                .filter(item -> !Boolean.TRUE.equals(overdueOnly) || item.isOverdue())
                .toList();

        return AutopilotQueuePageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(items.size())
                .totalPages(items.isEmpty() ? 0 : 1)
                .last(true)
                .build();
    }

    private AutopilotQueueItemResponse toQueueItem(AutopilotDispatchQueue queue) {
        UserProfile profile = queue.getUserProfile();

        ParentProfile parent = queue.getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(queue.getUserAccount().getId())
                .orElse(null)
                : null;

        boolean overdue = queue.getNextDispatchDueAt() != null
                && queue.getNextDispatchDueAt().isBefore(Instant.now())
                && queue.getQueueStatus() != AutopilotQueueStatus.DISPATCHED
                && queue.getQueueStatus() != AutopilotQueueStatus.COMPLETED
                && queue.getQueueStatus() != AutopilotQueueStatus.BLOCKED;

        EmployeeAccount assigned = queue.getAssignedEmployee();

        return AutopilotQueueItemResponse.builder()
                .queueId(queue.getId())
                .userId(queue.getUserAccount() != null ? queue.getUserAccount().getId() : null)
                .profileId(profile != null ? profile.getId() : null)
                .crmCaseId(queue.getCrmCase() != null ? queue.getCrmCase().getId() : null)

                .candidateName(profile != null ? profile.getCandidateFirstName() : null)
                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)
                .side(queue.getUserAccount() != null && queue.getUserAccount().getSide() != null
                        ? queue.getUserAccount().getSide().name()
                        : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)

                .assignedEmployeeId(assigned != null ? assigned.getId() : null)
                .assignedEmployeeName(queue.getAssignedEmployeeName())

                .planCode(queue.getPlanCode())
                .planName(queue.getPlanName())
                .paymentStatus(queue.getPaymentStatus())
                .subscriptionStatus(queue.getSubscriptionStatus())

                .profileCompletionPct(profile != null && profile.getCompletionPct() != null
                        ? profile.getCompletionPct().intValue()
                        : null)
                .verified(profile != null
                        && profile.getProfileStatus() != null
                        && ("LIVE".equalsIgnoreCase(profile.getProfileStatus().name())
                        || "VERIFIED".equalsIgnoreCase(profile.getProfileStatus().name())))
                .hasProfilePhoto(false)
                .photoSharingConsent("AFTER_ACCEPT")

                .queueStatus(queue.getQueueStatus())
                .priorityScore(queue.getPriorityScore())
                .reason(queue.getReason())

                .lastDispatchAt(queue.getLastDispatchAt())
                .nextDispatchDueAt(queue.getNextDispatchDueAt())
                .dispatchCount(queue.getDispatchCount())
                .pendingResponseCount(queue.getPendingResponseCount())

                .blockedReason(queue.getBlockedReason())
                .overdue(overdue)

                .createdAt(queue.getCreatedAt())
                .updatedAt(queue.getUpdatedAt())
                .build();
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "priorityScore");
        }

        String normalized = sort.trim().toLowerCase();

        if (normalized.contains("nextdispatchdueat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "nextDispatchDueAt")
                    : Sort.by(Sort.Direction.DESC, "nextDispatchDueAt");
        }

        if (normalized.contains("updatedat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "updatedAt")
                    : Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        return Sort.by(Sort.Direction.DESC, "priorityScore");
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isBlank()
                ? null
                : value.trim();
    }


    @Transactional(readOnly = true)
    public AutopilotSummaryResponse getSummary(
            LocalDate fromDate,
            LocalDate toDate,
            UUID assignedEmployeeId
    ) {
        UUID effectiveEmployeeId =
                permissionService.effectiveAssignedEmployeeId(assignedEmployeeId);

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        var rows = queueRepository.findSummarySource(
                effectiveEmployeeId,
                from,
                to
        );

        Instant now = Instant.now();

        long totalQueue = rows.size();

        long ready = 0;
        long pending = 0;
        long overdue = 0;
        long blocked = 0;
        long responsesPending = 0;
        long premiumDue = 0;
        long freeDue = 0;

        for (AutopilotDispatchQueue q : rows) {
            if (q.getQueueStatus() == AutopilotQueueStatus.READY) {
                ready++;
            }

            if (q.getQueueStatus() == AutopilotQueueStatus.PENDING) {
                pending++;
            }

            if (q.getQueueStatus() == AutopilotQueueStatus.BLOCKED) {
                blocked++;
            }

            if (q.getNextDispatchDueAt() != null
                    && q.getNextDispatchDueAt().isBefore(now)
                    && q.getQueueStatus() != AutopilotQueueStatus.DISPATCHED
                    && q.getQueueStatus() != AutopilotQueueStatus.COMPLETED
                    && q.getQueueStatus() != AutopilotQueueStatus.BLOCKED) {
                overdue++;
            }

            if (q.getPendingResponseCount() != null) {
                responsesPending += q.getPendingResponseCount();
            }

            if (q.getPlanCode() != null
                    && ("PREMIUM".equalsIgnoreCase(q.getPlanCode())
                    || "ELITE".equalsIgnoreCase(q.getPlanCode()))
                    && q.getNextDispatchDueAt() != null
                    && q.getNextDispatchDueAt().isBefore(now)) {
                premiumDue++;
            }

            if (q.getPlanCode() == null
                    || "FREE_ONBOARDING".equalsIgnoreCase(q.getPlanCode())
                    || "BASIC".equalsIgnoreCase(q.getPlanCode())) {
                if (q.getNextDispatchDueAt() != null
                        && q.getNextDispatchDueAt().isBefore(now)) {
                    freeDue++;
                }
            }
        }

        LocalDate today = LocalDate.now(ZoneId.systemDefault());

        Instant startOfDay = today
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant endOfDay = today
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        LocalDate weekStartDate = today.with(DayOfWeek.MONDAY);

        Instant weekStart = weekStartDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        long dispatchedToday = queueRepository.countDispatchedToday(
                effectiveEmployeeId,
                startOfDay,
                endOfDay
        );

        long skippedThisWeek = queueRepository.countSkippedThisWeek(
                effectiveEmployeeId,
                weekStart
        );

        return AutopilotSummaryResponse.builder()
                .totalQueue(totalQueue)
                .ready(ready)
                .pending(pending)
                .dispatchedToday(dispatchedToday)
                .overdue(overdue)
                .blocked(blocked)
                .responsesPending(responsesPending)
                .interestedResponses(0)
                .acceptedResponses(0)
                .skippedThisWeek(skippedThisWeek)
                .premiumDue(premiumDue)
                .freeDue(freeDue)
                .build();
    }

    @Transactional(readOnly = true)
    public AutopilotDispatchSuggestionsResponse getSuggestions(
            UUID queueId,
            int limit,
            Boolean includeAlreadySent
    ) {
        AutopilotDispatchQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch queue not found"));

        permissionService.assertCanViewQueue(queue);

        UserProfile sourceProfile = queue.getUserProfile();

        if (sourceProfile == null || sourceProfile.getUserAccount() == null) {
            throw new IllegalStateException("Queue source profile is invalid");
        }

        ParentProfile sourceParent = parentProfileRepository
                .findByUserAccountId(queue.getUserAccount().getId())
                .orElse(null);

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        Pageable pageable = PageRequest.of(0, safeLimit * 3);

        Page<UserProfile> candidates = userProfileRepository.findAllEligibleMatches(
                queue.getUserAccount().getSide(),
                sourceProfile.getId(),
                pageable
        );

        List<AutopilotSuggestionItemResponse> suggestions = candidates
                .getContent()
                .stream()
                .map(candidate -> toDispatchSuggestion(queue, sourceProfile, candidate))
                .filter(item -> Boolean.TRUE.equals(includeAlreadySent) || !item.isAlreadyDispatched())
                .limit(safeLimit)
                .toList();

        return AutopilotDispatchSuggestionsResponse.builder()
                .sourceProfile(
                        AutopilotSuggestionSourceProfileResponse.builder()
                                .userId(queue.getUserAccount().getId())
                                .profileId(sourceProfile.getId())
                                .candidateName(sourceProfile.getCandidateFirstName())
                                .side(queue.getUserAccount().getSide() != null
                                        ? queue.getUserAccount().getSide().name()
                                        : null)
                                .district(sourceParent != null ? sourceParent.getDistrict() : null)
                                .planName(queue.getPlanName())
                                .build()
                )
                .suggestions(suggestions)
                .build();
    }

    private AutopilotSuggestionItemResponse toDispatchSuggestion(
            AutopilotDispatchQueue queue,
            UserProfile sourceProfile,
            UserProfile candidate
    ) {
        ParentProfile parent = candidate.getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(candidate.getUserAccount().getId())
                .orElse(null)
                : null;

        boolean alreadyDispatched = dispatchItemRepository
                .existsBySourceProfile_IdAndCandidateProfile_Id(
                        sourceProfile.getId(),
                        candidate.getId()
                );

        boolean alreadyProposedForward = proposalRepository
                .existsByFromProfile_IdAndToProfile_Id(
                        sourceProfile.getId(),
                        candidate.getId()
                );

        boolean alreadyProposedReverse = proposalRepository
                .existsByToProfile_IdAndFromProfile_Id(
                        sourceProfile.getId(),
                        candidate.getId()
                );

        boolean alreadyProposed = alreadyProposedForward || alreadyProposedReverse;

        int score = calculateSimpleDispatchScore(queue, candidate, parent);

        List<String> reasons = new ArrayList<>();
        List<String> riskFlags = new ArrayList<>();

        if (parent != null && queue.getUserAccount() != null) {
            reasons.add("Profile eligible for dispatch");
        }

        if (candidate.getCompletionPct() != null && candidate.getCompletionPct().intValue() < 70) {
            riskFlags.add("Profile completion is low");
        }

        boolean canDispatch = !alreadyDispatched && !alreadyProposed;

        String blockReason = null;

        if (alreadyDispatched) {
            blockReason = "This profile was already dispatched to this family.";
        } else if (alreadyProposed) {
            blockReason = "Proposal already exists for this profile pair.";
        }

        return AutopilotSuggestionItemResponse.builder()
                .profileId(candidate.getId())
                .userId(candidate.getUserAccount() != null
                        ? candidate.getUserAccount().getId()
                        : null)

                .candidateName(candidate.getCandidateFirstName())
                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)

                .side(candidate.getUserAccount() != null
                        && candidate.getUserAccount().getSide() != null
                        ? candidate.getUserAccount().getSide().name()
                        : null)

                .age(candidate.getCandidateAge() != null
                        ? candidate.getCandidateAge().intValue()
                        : null)

                .heightCm(candidate.getCandidateHeightCm() != null
                        ? candidate.getCandidateHeightCm().intValue()
                        : null)

                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .maslak(parent != null ? parent.getMaslak() : null)

                .education(candidate.getEducation())

                .professionType(candidate.getProfessionType())

                .professionTitle(candidate.getProfessionTitle())

                .monthlyIncome(candidate.getMonthlyIncome())

                .verified(candidate.getProfileStatus() != null
                        && ("LIVE".equalsIgnoreCase(candidate.getProfileStatus().name())
                        || "VERIFIED".equalsIgnoreCase(candidate.getProfileStatus().name())))

                .hasProfilePhoto(false)
                .profilePhotoViewUrl(null)

                .matchScore(score)
                .compatibilityScore(score)

                .dispatchReadiness(canDispatch ? "READY" : "BLOCKED")

                .alreadyDispatched(alreadyDispatched)
                .alreadyProposed(alreadyProposed)

                .canDispatch(canDispatch)
                .blockReason(blockReason)

                .reasons(reasons)
                .riskFlags(riskFlags)

                .build();
    }

    private int calculateSimpleDispatchScore(
            AutopilotDispatchQueue queue,
            UserProfile candidate,
            ParentProfile candidateParent
    ) {
        int score = 50;

        if (candidate.getProfileStatus() != null
                && "LIVE".equalsIgnoreCase(candidate.getProfileStatus().name())) {
            score += 15;
        }

        if (candidate.getCompletionPct() != null) {
            score += Math.min(20, candidate.getCompletionPct().intValue() / 5);
        }

        if (candidateParent != null && candidateParent.getDistrict() != null
                && queue.getUserAccount() != null) {
            score += 5;
        }

        return Math.min(score, 100);
    }

    @Transactional
    public DispatchPreviewResponse createPreview(
            CreateDispatchPreviewRequest request
    ) {
        AutopilotDispatchQueue queue = queueRepository.findById(request.getQueueId())
                .orElseThrow(() -> new EntityNotFoundException("Dispatch queue not found"));

        permissionService.assertCanViewQueue(queue);

        if (queue.getUserProfile() == null
                || !queue.getUserProfile().getId().equals(request.getSourceProfileId())) {
            throw new IllegalArgumentException("Source profile does not match queue");
        }

        if (request.getCandidateProfileIds().size() > 3) {
            throw new IllegalArgumentException("Maximum 3 profiles can be dispatched at once");
        }

        List<DispatchPreviewItemResponse> items = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean canSend = true;
        String blockReason = null;

        for (UUID candidateProfileId : request.getCandidateProfileIds()) {
            UserProfile candidate = userProfileRepository.findById(candidateProfileId)
                    .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found"));

            boolean alreadyDispatched = dispatchItemRepository
                    .existsBySourceProfile_IdAndCandidateProfile_Id(
                            request.getSourceProfileId(),
                            candidateProfileId
                    );

            boolean alreadyProposedForward = proposalRepository
                    .existsByFromProfile_IdAndToProfile_Id(
                            request.getSourceProfileId(),
                            candidateProfileId
                    );

            boolean alreadyProposedReverse = proposalRepository
                    .existsByToProfile_IdAndFromProfile_Id(
                            request.getSourceProfileId(),
                            candidateProfileId
                    );

            if (alreadyDispatched || alreadyProposedForward || alreadyProposedReverse) {
                canSend = false;
                blockReason = "One or more selected profiles were already dispatched or proposed.";
            }

            ParentProfile parent = candidate.getUserAccount() != null
                    ? parentProfileRepository
                    .findByUserAccountId(candidate.getUserAccount().getId())
                    .orElse(null)
                    : null;

            boolean photoIncluded = request.isShareProfilePhoto();

            String photoBlockedReason = null;

            if (request.isShareProfilePhoto()) {
                warnings.add("Photo sharing will follow current family consent rules.");
            }

            items.add(
                    DispatchPreviewItemResponse.builder()
                            .candidateProfileId(candidate.getId())
                            .candidateName(candidate.getCandidateFirstName())
                            .summaryText(buildCandidateSummary(candidate, parent))
                            .matchScore(calculateSimpleDispatchScore(queue, candidate, parent))
                            .photoIncluded(photoIncluded)
                            .photoBlockedReason(photoBlockedReason)
                            .build()
            );
        }

        String messagePreview = buildDispatchMessagePreview(
                queue,
                items,
                request.getNote()
        );

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        AutopilotDispatchDraft draft = new AutopilotDispatchDraft();
        draft.setQueue(queue);
        draft.setSourceProfile(queue.getUserProfile());
        draft.setChannel(request.getChannel());
        draft.setShareProfilePhoto(request.isShareProfilePhoto());
        draft.setNote(request.getNote());
        draft.setMessagePreview(messagePreview);
        draft.setCandidateProfileIds(
                request.getCandidateProfileIds()
                        .stream()
                        .map(UUID::toString)
                        .collect(Collectors.joining(","))
        );
        draft.setWarnings(String.join(" | ", warnings));
        draft.setCanSend(canSend);
        draft.setBlockReason(blockReason);
        draft.setCreatedByEmployee(employee);
        draft.setCreatedByName(employee != null ? employee.getFullName() : "System");

        AutopilotDispatchDraft saved = draftRepository.save(draft);

        createAutopilotTimelineEvent(
                queue,
                CrmTimelineEventType.AUTOPILOT_DISPATCH_PREVIEWED,
                "Autopilot dispatch preview generated",
                "Dispatch preview generated with "
                        + request.getCandidateProfileIds().size()
                        + " candidate profile(s)."
        );

        createAutopilotAuditEvent(
                AuditAction.AUTOPILOT_DISPATCH_PREVIEWED,
                queue.getUserProfile().getId(),
                "Autopilot dispatch preview generated"
        );

        return DispatchPreviewResponse.builder()
                .draftId(saved.getId())
                .queueId(queue.getId())
                .sourceProfileId(queue.getUserProfile().getId())
                .channel(saved.getChannel())
                .shareProfilePhoto(saved.isShareProfilePhoto())
                .messagePreview(saved.getMessagePreview())
                .items(items)
                .warnings(warnings)
                .canSend(saved.isCanSend())
                .blockReason(saved.getBlockReason())
                .build();
    }

    private String buildCandidateSummary(
            UserProfile candidate,
            ParentProfile parent
    ) {
        List<String> parts = new ArrayList<>();

        if (candidate.getCandidateFirstName() != null) {
            parts.add(candidate.getCandidateFirstName());
        }

        if (candidate.getCandidateAge() != null) {
            parts.add(candidate.getCandidateAge() + " years");
        }

        if (parent != null && parent.getDistrict() != null) {
            parts.add(parent.getDistrict());
        }

        if (candidate.getEducation() != null) {
            parts.add(candidate.getEducation());
        }

        if (candidate.getProfessionTitle() != null) {
            parts.add(candidate.getProfessionTitle());
        }

        return String.join(", ", parts);
    }


    private String buildDispatchMessagePreview(
            AutopilotDispatchQueue queue,
            List<DispatchPreviewItemResponse> items,
            String note
    ) {
        String parentName = "Family";

        ParentProfile parent = queue.getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(queue.getUserAccount().getId())
                .orElse(null)
                : null;

        if (parent != null && parent.getParentName() != null) {
            parentName = parent.getParentName();
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Assalamu Alaikum ")
                .append(parentName)
                .append(",\n\n");

        builder.append("Here are ")
                .append(items.size())
                .append(" suitable rishta profile");

        if (items.size() > 1) {
            builder.append("s");
        }

        builder.append(" for your family:\n\n");

        int index = 1;

        for (DispatchPreviewItemResponse item : items) {
            builder.append(index++)
                    .append(". ")
                    .append(item.getSummaryText())
                    .append("\n");
        }

        if (note != null && !note.isBlank()) {
            builder.append("\nNote: ")
                    .append(note.trim())
                    .append("\n");
        }

        builder.append("\nPlease let us know which profile you would like to discuss further.");

        return builder.toString();
    }

    @Transactional
    public SendDispatchResponse sendDispatch(
            UUID draftId,
            SendDispatchRequest request
    ) {
        AutopilotDispatchDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch draft not found"));

        AutopilotDispatchQueue queue = draft.getQueue();

        permissionService.assertCanViewQueue(queue);

        if (!draft.isCanSend()) {
            throw new IllegalStateException(
                    draft.getBlockReason() != null
                            ? draft.getBlockReason()
                            : "Dispatch draft cannot be sent"
            );
        }

        List<UUID> candidateIds = parseCandidateIds(draft.getCandidateProfileIds());

        if (candidateIds.isEmpty()) {
            throw new IllegalStateException("Dispatch draft has no candidate profiles");
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        Instant now = Instant.now();

        AutopilotDispatchBatch batch = new AutopilotDispatchBatch();
        batch.setQueue(queue);
        batch.setSourceProfile(draft.getSourceProfile());
        batch.setStatus(AutopilotBatchStatus.SENT);
        batch.setChannel(draft.getChannel());
        batch.setSendMode(
                request.getSendMode() != null
                        ? request.getSendMode()
                        : AutopilotSendMode.MANUAL_WHATSAPP
        );
        batch.setShareProfilePhoto(draft.isShareProfilePhoto());
        batch.setNote(
                request.getNote() != null && !request.getNote().isBlank()
                        ? request.getNote().trim()
                        : draft.getNote()
        );
        batch.setItemsCount(candidateIds.size());
        batch.setSentByEmployee(employee);
        batch.setSentByName(employee != null ? employee.getFullName() : "System");
        batch.setSentAt(now);

        AutopilotDispatchBatch savedBatch = batchRepository.save(batch);

        List<UUID> proposalIds = new ArrayList<>();

        for (UUID candidateId : candidateIds) {
            UserProfile candidate = userProfileRepository.findById(candidateId)
                    .orElseThrow(() -> new EntityNotFoundException("Candidate profile not found"));

            validateNoActiveDuplicateDispatch(
                    draft.getSourceProfile().getId(),
                    candidate.getId()
            );

            Proposal proposal = null;

            if (request.isCreateProposals()) {
                proposal = createProposalFromDispatch(
                        queue,
                        draft.getSourceProfile(),
                        candidate,
                        savedBatch
                );

                proposalIds.add(proposal.getId());
            }

            AutopilotDispatchItem item = new AutopilotDispatchItem();
            item.setDispatchBatch(savedBatch);
            item.setQueue(queue);
            item.setSourceProfile(draft.getSourceProfile());
            item.setCandidateProfile(candidate);
            item.setProposal(proposal);
            item.setStatus(AutopilotDispatchItemStatus.SENT);
            item.setResponseStatus(AutopilotResponseStatus.NO_RESPONSE);
            item.setMatchScore(80);
            item.setCompatibilityScore(80);
            item.setDispatchReadiness("SENT");
            item.setPhotoIncluded(draft.isShareProfilePhoto());

            if (proposal != null) {
                createAutopilotAuditEvent(
                        AuditAction.AUTOPILOT_PROPOSAL_CREATED,
                        proposal.getId(),
                        "Proposal created from autopilot dispatch"
                );
            }

            dispatchItemRepository.save(item);
            createAutopilotTimelineEvent(
                    queue,
                    CrmTimelineEventType.AUTOPILOT_DISPATCH_SENT,
                    "Autopilot dispatch sent",
                    "Dispatch marked as sent with "
                            + candidateIds.size()
                            + " candidate profile(s)."
            );

            createAutopilotAuditEvent(
                    AuditAction.AUTOPILOT_DISPATCH_SENT,
                    queue.getUserProfile().getId(),
                    "Autopilot dispatch sent"
            );
        }

        queue.setQueueStatus(AutopilotQueueStatus.DISPATCHED);
        queue.setLastDispatchAt(now);
        queue.setDispatchCount(
                queue.getDispatchCount() != null
                        ? queue.getDispatchCount() + 1
                        : 1
        );
        queue.setPendingResponseCount(
                queue.getPendingResponseCount() != null
                        ? queue.getPendingResponseCount() + candidateIds.size()
                        : candidateIds.size()
        );
        queue.setNextDispatchDueAt(now.plusSeconds(resolveNextDispatchSeconds(queue)));

        queueRepository.save(queue);

        return SendDispatchResponse.builder()
                .dispatchBatchId(savedBatch.getId())
                .queueId(queue.getId())
                .status(savedBatch.getStatus())
                .sentAt(savedBatch.getSentAt())
                .itemsCount(savedBatch.getItemsCount())
                .proposalIds(proposalIds)
                .pipelineCreated(request.isCreateProposals())
                .build();
    }

    private List<UUID> parseCandidateIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .map(UUID::fromString)
                .toList();
    }

    private void validateNoActiveDuplicateDispatch(
            UUID sourceProfileId,
            UUID candidateProfileId
    ) {
        Collection<AutopilotDispatchItemStatus> activeStatuses = List.of(
                AutopilotDispatchItemStatus.SENT,
                AutopilotDispatchItemStatus.VIEWED,
                AutopilotDispatchItemStatus.INTERESTED,
                AutopilotDispatchItemStatus.ACCEPTED
        );

        boolean alreadyDispatched = dispatchItemRepository
                .existsBySourceProfile_IdAndCandidateProfile_IdAndStatusIn(
                        sourceProfileId,
                        candidateProfileId,
                        activeStatuses
                );

        if (alreadyDispatched) {
            throw new IllegalStateException("This profile pair was already dispatched");
        }

        boolean proposedForward = proposalRepository.existsByFromProfile_IdAndToProfile_Id(
                sourceProfileId,
                candidateProfileId
        );

        boolean proposedReverse = proposalRepository.existsByToProfile_IdAndFromProfile_Id(
                sourceProfileId,
                candidateProfileId
        );

        if (proposedForward || proposedReverse) {
            throw new IllegalStateException("Proposal already exists for this profile pair");
        }
    }

    private Proposal createProposalFromDispatch(
            AutopilotDispatchQueue queue,
            UserProfile sourceProfile,
            UserProfile candidate,
            AutopilotDispatchBatch batch
    ) {
        EmployeeAccount employee = permissionService.getCurrentEmployee();

        Proposal proposal = new Proposal();
        proposal.setFromProfile(sourceProfile);
        proposal.setToProfile(candidate);
        proposal.setCrmCase(queue.getCrmCase());
        proposal.setStatus(ProposalStatus.SENT);
        proposal.setDispatchChannel(ProposalDispatchChannel.CRM_CALL);
        proposal.setSourceType(ProposalSourceType.ASSISTED_DISPATCH);
        proposal.setNote("Created from Autopilot Dispatch Centre");
        proposal.setMatchScore(80);
        proposal.setShareProfilePhoto(batch.isShareProfilePhoto());
        proposal.setDispatchedByEmployee(employee);
        proposal.setDispatchedByName(employee != null ? employee.getFullName() : "System");
        proposal.setDispatchedAt(Instant.now());
        proposal.setLastUpdatedByEmployee(employee);
        proposal.setLastUpdatedByName(employee != null ? employee.getFullName() : "System");

        return proposalRepository.save(proposal);
    }

    private long resolveNextDispatchSeconds(AutopilotDispatchQueue queue) {
        if (queue.getPlanCode() == null) {
            return 14L * 24 * 60 * 60;
        }

        return switch (queue.getPlanCode().toUpperCase()) {
            case "ELITE" -> 3L * 24 * 60 * 60;
            case "PREMIUM" -> 7L * 24 * 60 * 60;
            case "BASIC" -> 10L * 24 * 60 * 60;
            default -> 14L * 24 * 60 * 60;
        };
    }

    @Transactional
    public QueueActionResponse skipQueue(
            UUID queueId,
            SkipDispatchQueueRequest request
    ) {
        AutopilotDispatchQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch queue not found"));

        permissionService.assertCanViewQueue(queue);

        Instant nextDue = Instant.now().plusSeconds(resolveNextDispatchSeconds(queue));

        queue.setQueueStatus(AutopilotQueueStatus.SKIPPED);
        queue.setReason(request.getReason().trim());
        queue.setNextDispatchDueAt(nextDue);

        AutopilotDispatchQueue saved = queueRepository.save(queue);

        createAutopilotTimelineEvent(
                saved,
                CrmTimelineEventType.AUTOPILOT_DISPATCH_SKIPPED,
                "Autopilot dispatch skipped",
                saved.getReason()
        );

        createAutopilotAuditEvent(
                AuditAction.AUTOPILOT_DISPATCH_SKIPPED,
                saved.getUserProfile().getId(),
                "Autopilot dispatch skipped: " + saved.getReason()
        );


        return QueueActionResponse.builder()
                .queueId(saved.getId())
                .queueStatus(saved.getQueueStatus())
                .nextDispatchDueAt(saved.getNextDispatchDueAt())
                .reason(saved.getReason())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public QueueActionResponse postponeQueue(
            UUID queueId,
            PostponeDispatchQueueRequest request
    ) {
        AutopilotDispatchQueue queue = queueRepository.findById(queueId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch queue not found"));

        permissionService.assertCanViewQueue(queue);

        if (request.getNextDispatchDueAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Next dispatch due time must be in the future");
        }

        queue.setQueueStatus(AutopilotQueueStatus.POSTPONED);
        queue.setReason(
                request.getReason() != null && !request.getReason().isBlank()
                        ? request.getReason().trim()
                        : "Dispatch postponed"
        );
        queue.setNextDispatchDueAt(request.getNextDispatchDueAt());

        AutopilotDispatchQueue saved = queueRepository.save(queue);

        createAutopilotTimelineEvent(
                saved,
                CrmTimelineEventType.AUTOPILOT_DISPATCH_POSTPONED,
                "Autopilot dispatch postponed",
                "Next dispatch due at " + saved.getNextDispatchDueAt()
        );

        createAutopilotAuditEvent(
                AuditAction.AUTOPILOT_DISPATCH_POSTPONED,
                saved.getUserProfile().getId(),
                "Autopilot dispatch postponed"
        );


        return QueueActionResponse.builder()
                .queueId(saved.getId())
                .queueStatus(saved.getQueueStatus())
                .nextDispatchDueAt(saved.getNextDispatchDueAt())
                .reason(saved.getReason())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public DispatchHistoryPageResponse getDispatchHistory(
            int page,
            int size,
            String search,
            AutopilotBatchStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            String sort
    ) {
        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        Page<AutopilotDispatchBatch> result =
                batchRepository.searchHistory(
                        blankToNull(search),
                        status != null ? status.name() : null,
                        from,
                        to,
                        pageable
                );

        List<DispatchHistoryItemResponse> items = result.getContent()
                .stream()
                .map(this::toDispatchHistoryItem)
                .toList();

        return DispatchHistoryPageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public DispatchDetailResponse getDispatchDetail(UUID dispatchBatchId) {
        AutopilotDispatchBatch batch = batchRepository.findById(dispatchBatchId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch batch not found"));

        permissionService.assertCanViewQueue(batch.getQueue());

        ParentProfile sourceParent = batch.getQueue() != null
                && batch.getQueue().getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(batch.getQueue().getUserAccount().getId())
                .orElse(null)
                : null;

        List<AutopilotDispatchItem> dispatchItems =
                dispatchItemRepository.findByDispatchBatch_Id(dispatchBatchId);

        List<DispatchDetailItemResponse> items = dispatchItems
                .stream()
                .map(this::toDispatchDetailItem)
                .toList();

        return DispatchDetailResponse.builder()
                .dispatchBatchId(batch.getId())
                .queueId(batch.getQueue() != null ? batch.getQueue().getId() : null)
                .sourceProfileId(batch.getSourceProfile() != null
                        ? batch.getSourceProfile().getId()
                        : null)

                .sourceCandidateName(batch.getSourceProfile() != null
                        ? batch.getSourceProfile().getCandidateFirstName()
                        : null)

                .sourceParentName(sourceParent != null
                        ? sourceParent.getParentName()
                        : null)

                .sourcePhone(sourceParent != null
                        ? sourceParent.getParentPhone()
                        : null)

                .status(batch.getStatus())
                .channel(batch.getChannel())
                .sendMode(batch.getSendMode())

                .shareProfilePhoto(batch.isShareProfilePhoto())

                .note(batch.getNote())

                .itemsCount(batch.getItemsCount())

                .sentByName(batch.getSentByName())

                .sentAt(batch.getSentAt())

                .providerName(batch.getProviderName())
                .providerMessageId(batch.getProviderMessageId())
                .deliveryStatus(batch.getDeliveryStatus())

                .deliveredAt(batch.getDeliveredAt())
                .readAt(batch.getReadAt())
                .repliedAt(batch.getRepliedAt())

                .items(items)

                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())

                .build();
    }

    private DispatchHistoryItemResponse toDispatchHistoryItem(
            AutopilotDispatchBatch batch
    ) {
        ParentProfile parent = batch.getQueue() != null
                && batch.getQueue().getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(batch.getQueue().getUserAccount().getId())
                .orElse(null)
                : null;

        int interested = (int) dispatchItemRepository.countByBatchAndResponse(
                batch.getId(),
                com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus.INTERESTED
        );

        int accepted = (int) dispatchItemRepository.countByBatchAndResponse(
                batch.getId(),
                com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus.ACCEPTED
        );

        int rejected = (int) dispatchItemRepository.countByBatchAndResponse(
                batch.getId(),
                com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus.REJECTED
        );

        int noResponses = Math.max(
                0,
                (batch.getItemsCount() != null ? batch.getItemsCount() : 0)
                        - interested
                        - accepted
                        - rejected
        );

        return DispatchHistoryItemResponse.builder()
                .dispatchBatchId(batch.getId())
                .queueId(batch.getQueue() != null ? batch.getQueue().getId() : null)
                .sourceProfileId(batch.getSourceProfile() != null
                        ? batch.getSourceProfile().getId()
                        : null)

                .sourceCandidateName(batch.getSourceProfile() != null
                        ? batch.getSourceProfile().getCandidateFirstName()
                        : null)

                .sourceParentName(parent != null ? parent.getParentName() : null)
                .sourcePhone(parent != null ? parent.getParentPhone() : null)

                .status(batch.getStatus())
                .channel(batch.getChannel())
                .sendMode(batch.getSendMode())

                .itemsCount(batch.getItemsCount())

                .sentByName(batch.getSentByName())
                .sentAt(batch.getSentAt())

                .providerName(batch.getProviderName())
                .deliveryStatus(batch.getDeliveryStatus())

                .interestedResponses(interested)
                .acceptedResponses(accepted)
                .rejectedResponses(rejected)
                .noResponses(noResponses)

                .createdAt(batch.getCreatedAt())

                .build();
    }

    private DispatchDetailItemResponse toDispatchDetailItem(
            AutopilotDispatchItem item
    ) {
        ParentProfile parent = item.getCandidateProfile() != null
                && item.getCandidateProfile().getUserAccount() != null
                ? parentProfileRepository
                .findByUserAccountId(item.getCandidateProfile().getUserAccount().getId())
                .orElse(null)
                : null;

        return DispatchDetailItemResponse.builder()
                .dispatchItemId(item.getId())

                .candidateProfileId(item.getCandidateProfile() != null
                        ? item.getCandidateProfile().getId()
                        : null)

                .proposalId(item.getProposal() != null
                        ? item.getProposal().getId()
                        : null)

                .candidateName(item.getCandidateProfile() != null
                        ? item.getCandidateProfile().getCandidateFirstName()
                        : null)

                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)

                .district(parent != null ? parent.getDistrict() : null)

                .matchScore(item.getMatchScore())
                .compatibilityScore(item.getCompatibilityScore())

                .dispatchReadiness(item.getDispatchReadiness())

                .status(item.getStatus())
                .responseStatus(item.getResponseStatus())

                .photoIncluded(item.isPhotoIncluded())

                .providerName(item.getProviderName())
                .providerMessageId(item.getProviderMessageId())
                .deliveryStatus(item.getDeliveryStatus())

                .deliveredAt(item.getDeliveredAt())
                .readAt(item.getReadAt())
                .repliedAt(item.getRepliedAt())

                .responseNote(item.getResponseNote())
                .respondedAt(item.getRespondedAt())

                .build();
    }

    @Transactional
    public RecordDispatchResponseResponse recordResponse(
            UUID dispatchItemId,
            RecordDispatchResponseRequest request
    ) {
        AutopilotDispatchItem item = dispatchItemRepository.findById(dispatchItemId)
                .orElseThrow(() -> new EntityNotFoundException("Dispatch item not found"));

        permissionService.assertCanViewQueue(item.getQueue());

        item.setResponseStatus(request.getResponseStatus());
        item.setResponseNote(
                request.getNote() != null && !request.getNote().isBlank()
                        ? request.getNote().trim()
                        : null
        );
        item.setRespondedAt(Instant.now());

        item.setStatus(resolveItemStatusFromResponse(request.getResponseStatus()));

        Proposal proposal = item.getProposal();

        if (proposal != null) {
            ProposalStatus proposalStatus = resolveProposalStatusFromResponse(
                    request.getResponseStatus()
            );

            proposal.setStatus(proposalStatus);
            proposal.setUpdatedAt(Instant.now());

            proposalRepository.save(proposal);


        }

        AutopilotDispatchQueue queue = item.getQueue();

        if (queue != null && queue.getPendingResponseCount() != null && queue.getPendingResponseCount() > 0) {
            queue.setPendingResponseCount(queue.getPendingResponseCount() - 1);
            queueRepository.save(queue);
        }

        if (request.getNextFollowUpAt() != null) {
            createFollowUpFromDispatchResponse(item, request);
        }

        AutopilotDispatchItem saved = dispatchItemRepository.save(item);

        createAutopilotTimelineEvent(
                saved.getQueue(),
                CrmTimelineEventType.AUTOPILOT_RESPONSE_RECORDED,
                "Autopilot response recorded",
                "Response recorded as " + saved.getResponseStatus()
        );

        createAutopilotAuditEvent(
                AuditAction.AUTOPILOT_RESPONSE_RECORDED,
                saved.getSourceProfile().getId(),
                "Autopilot response recorded: " + saved.getResponseStatus()
        );

        updateBatchResponseStatus(saved.getDispatchBatch());

        RishtaPipelineStage stage = proposal != null
                ? pipelineStageResolver.resolve(proposal.getStatus())
                : null;

        return RecordDispatchResponseResponse.builder()
                .dispatchItemId(saved.getId())
                .responseStatus(saved.getResponseStatus())
                .proposalId(proposal != null ? proposal.getId() : null)
                .pipelineStage(stage)
                .nextFollowUpAt(request.getNextFollowUpAt())
                .build();
    }

    private AutopilotDispatchItemStatus resolveItemStatusFromResponse(
            AutopilotResponseStatus responseStatus
    ) {
        if (responseStatus == null) {
            return AutopilotDispatchItemStatus.SENT;
        }

        return switch (responseStatus) {
            case INTERESTED, ASKED_FOR_MORE_DETAILS, CALL_BACK, FOLLOW_UP_REQUIRED ->
                    AutopilotDispatchItemStatus.INTERESTED;

            case ACCEPTED ->
                    AutopilotDispatchItemStatus.ACCEPTED;

            case REJECTED ->
                    AutopilotDispatchItemStatus.REJECTED;

            case NOT_INTERESTED, WRONG_CONTACT ->
                    AutopilotDispatchItemStatus.NOT_INTERESTED;

            case NO_RESPONSE ->
                    AutopilotDispatchItemStatus.SENT;
        };
    }

    private ProposalStatus resolveProposalStatusFromResponse(
            AutopilotResponseStatus responseStatus
    ) {
        if (responseStatus == null) {
            return ProposalStatus.SENT;
        }

        return switch (responseStatus) {
            case INTERESTED, ASKED_FOR_MORE_DETAILS, CALL_BACK, FOLLOW_UP_REQUIRED ->
                    ProposalStatus.INTERESTED;

            case ACCEPTED ->
                    ProposalStatus.ACCEPTED;

            case REJECTED ->
                    ProposalStatus.REJECTED;

            case NOT_INTERESTED, WRONG_CONTACT ->
                    ProposalStatus.NOT_INTERESTED;

            case NO_RESPONSE ->
                    ProposalStatus.SENT;
        };
    }

    private void createFollowUpFromDispatchResponse(
            AutopilotDispatchItem item,
            RecordDispatchResponseRequest request
    ) {
        if (item.getQueue() == null || item.getQueue().getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        CrmFollowUp followUp = new CrmFollowUp();
        followUp.setCrmCase(item.getQueue().getCrmCase());
        followUp.setProposal(item.getProposal());
        followUp.setAssignedEmployee(
                item.getQueue().getAssignedEmployee() != null
                        ? item.getQueue().getAssignedEmployee()
                        : employee
        );
        followUp.setScheduledAt(request.getNextFollowUpAt());
        followUp.setStatus(CrmFollowUpStatus.SCHEDULED);
        followUp.setChannel(CrmFollowUpChannel.PHONE);
        followUp.setPurpose(
                request.getNote() != null && !request.getNote().isBlank()
                        ? request.getNote().trim()
                        : "Dispatch response follow-up"
        );

        crmFollowUpRepository.save(followUp);

        item.getQueue().getCrmCase().setNextFollowUpAt(request.getNextFollowUpAt());
    }

    private void updateBatchResponseStatus(AutopilotDispatchBatch batch) {
        if (batch == null) {
            return;
        }

        List<AutopilotDispatchItem> items =
                dispatchItemRepository.findByDispatchBatch_Id(batch.getId());

        long responded = items.stream()
                .filter(i -> i.getResponseStatus() != null
                        && i.getResponseStatus() != AutopilotResponseStatus.NO_RESPONSE)
                .count();

        if (responded == 0) {
            return;
        }

        if (responded == items.size()) {
            batch.setStatus(AutopilotBatchStatus.RESPONDED);
        } else {
            batch.setStatus(AutopilotBatchStatus.PARTIALLY_RESPONDED);
        }

        batchRepository.save(batch);
    }


    @Transactional
    public GenerateDispatchQueueResponse generateQueue(
            GenerateDispatchQueueRequest request
    ) {
        int limit = request.getLimit() != null
                ? Math.min(Math.max(request.getLimit(), 1), 500)
                : 200;

        Pageable pageable = PageRequest.of(
                0,
                limit,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<UserProfile> profiles = userProfileRepository.findAll(pageable);

        int created = 0;
        int updated = 0;
        int skipped = 0;
        int blocked = 0;

        Instant dueBefore = request.getDueBefore() != null
                ? request.getDueBefore()
                : Instant.now();

        for (UserProfile profile : profiles.getContent()) {
            try {
                if (profile.getUserAccount() == null) {
                    skipped++;
                    continue;
                }

                if (!isProfileEligibleForDispatch(profile)) {
                    blocked++;
                    upsertBlockedQueue(profile, "Profile is not eligible for dispatch");
                    continue;
                }

                CrmCase crmCase = crmCaseRepository
                        .findTopByUserAccountIdOrderByUpdatedAtDesc(profile.getUserAccount().getId())
                        .orElse(null);

                if (request.getAssignedEmployeeId() != null) {
                    if (crmCase == null
                            || crmCase.getAssignedEmployee() == null
                            || !crmCase.getAssignedEmployee().getId().equals(request.getAssignedEmployeeId())) {
                        skipped++;
                        continue;
                    }
                }

                SubscriptionResponse subscription = null;

                try {
                    subscription = revenueService
                            .getFamilySubscription(profile.getUserAccount().getId())
                            .getCurrentSubscription();
                } catch (Exception ignored) {
                }

                if (request.getPlanCode() != null
                        && subscription != null
                        && subscription.getPlanCode() != null
                        && !subscription.getPlanCode().equalsIgnoreCase(request.getPlanCode())) {
                    skipped++;
                    continue;
                }

                AutopilotDispatchQueue queue = queueRepository
                        .findByUserProfile_Id(profile.getId())
                        .orElse(null);

                if (queue == null) {
                    queue = new AutopilotDispatchQueue();
                    queue.setUserAccount(profile.getUserAccount());
                    queue.setUserProfile(profile);
                    created++;
                } else {
                    updated++;
                }

                queue.setCrmCase(crmCase);

                if (crmCase != null && crmCase.getAssignedEmployee() != null) {
                    queue.setAssignedEmployee(crmCase.getAssignedEmployee());
                    queue.setAssignedEmployeeName(crmCase.getAssignedEmployee().getFullName());
                }

                applySubscriptionSnapshot(queue, subscription);

                queue.setPriorityScore(calculateQueuePriority(profile, subscription, crmCase));
                queue.setQueueStatus(resolveGeneratedQueueStatus(queue, dueBefore));
                queue.setReason(buildQueueReason(subscription));
                queue.setBlockedReason(null);

                if (queue.getNextDispatchDueAt() == null) {
                    queue.setNextDispatchDueAt(resolveInitialDueAt(subscription));
                }

                queueRepository.save(queue);

            } catch (Exception ex) {
                skipped++;
            }
        }

        auditLogService.record(
                AuditAction.AUTOPILOT_QUEUE_GENERATED,
                AuditEntityType.SYSTEM,
                permissionService.getCurrentEmployee().getId(),
                "Autopilot dispatch queue generated. Created: "
                        + created
                        + ", updated: "
                        + updated
                        + ", skipped: "
                        + skipped
                        + ", blocked: "
                        + blocked
        );


        return GenerateDispatchQueueResponse.builder()
                .created(created)
                .updated(updated)
                .skipped(skipped)
                .blocked(blocked)
                .build();
    }

    private boolean isProfileEligibleForDispatch(UserProfile profile) {
        if (profile == null || profile.getUserAccount() == null) {
            return false;
        }

        if (profile.getProfileStatus() == null) {
            return false;
        }

        String status = profile.getProfileStatus().name();

        return "LIVE".equalsIgnoreCase(status)
                || "VERIFIED".equalsIgnoreCase(status)
                || "APPROVED".equalsIgnoreCase(status);
    }

    private void upsertBlockedQueue(
            UserProfile profile,
            String reason
    ) {
        if (profile == null || profile.getUserAccount() == null) {
            return;
        }

        AutopilotDispatchQueue queue = queueRepository
                .findByUserProfile_Id(profile.getId())
                .orElseGet(AutopilotDispatchQueue::new);

        queue.setUserAccount(profile.getUserAccount());
        queue.setUserProfile(profile);
        queue.setQueueStatus(AutopilotQueueStatus.BLOCKED);
        queue.setBlockedReason(reason);
        queue.setPriorityScore(0);

        queueRepository.save(queue);
    }

    private void applySubscriptionSnapshot(
            AutopilotDispatchQueue queue,
            SubscriptionResponse subscription
    ) {
        if (subscription == null) {
            queue.setPlanCode("FREE_ONBOARDING");
            queue.setPlanName("Free Onboarding");
            queue.setPaymentStatus("NOT_REQUIRED");
            queue.setSubscriptionStatus("FREE");
            return;
        }

        queue.setPlanCode(subscription.getPlanCode());
        queue.setPlanName(subscription.getPlanName());
        queue.setPaymentStatus(subscription.getPaymentStatus() != null
                ? subscription.getPaymentStatus().name()
                : null);
        queue.setSubscriptionStatus(subscription.getSubscriptionStatus() != null
                ? subscription.getSubscriptionStatus().name()
                : null);
    }

    private int calculateQueuePriority(
            UserProfile profile,
            SubscriptionResponse subscription,
            CrmCase crmCase
    ) {
        int score = 40;

        if (subscription != null && subscription.getPlanCode() != null) {
            switch (subscription.getPlanCode().toUpperCase()) {
                case "ELITE" -> score += 40;
                case "PREMIUM" -> score += 30;
                case "BASIC" -> score += 15;
                default -> score += 5;
            }
        }

        if (profile.getCompletionPct() != null) {
            score += Math.min(20, profile.getCompletionPct().intValue() / 5);
        }

        if (crmCase != null && crmCase.getAssignedEmployee() != null) {
            score += 10;
        }

        return Math.min(score, 100);
    }

    private AutopilotQueueStatus resolveGeneratedQueueStatus(
            AutopilotDispatchQueue queue,
            Instant dueBefore
    ) {
        if (queue.getBlockedReason() != null && !queue.getBlockedReason().isBlank()) {
            return AutopilotQueueStatus.BLOCKED;
        }

        if (queue.getNextDispatchDueAt() == null) {
            return AutopilotQueueStatus.READY;
        }

        if (!queue.getNextDispatchDueAt().isAfter(dueBefore)) {
            return AutopilotQueueStatus.READY;
        }

        return AutopilotQueueStatus.PENDING;
    }
    private String buildQueueReason(SubscriptionResponse subscription) {
        if (subscription == null || subscription.getPlanCode() == null) {
            return "Free onboarding family pending dispatch review.";
        }

        return switch (subscription.getPlanCode().toUpperCase()) {
            case "ELITE" -> "Elite family due for priority dispatch.";
            case "PREMIUM" -> "Premium family pending weekly dispatch.";
            case "BASIC" -> "Basic family pending scheduled dispatch.";
            default -> "Family pending dispatch review.";
        };
    }

    private Instant resolveInitialDueAt(SubscriptionResponse subscription) {
        Instant now = Instant.now();

        if (subscription == null || subscription.getPlanCode() == null) {
            return now.plusSeconds(14L * 24 * 60 * 60);
        }

        return switch (subscription.getPlanCode().toUpperCase()) {
            case "ELITE" -> now;
            case "PREMIUM" -> now;
            case "BASIC" -> now.plusSeconds(3L * 24 * 60 * 60);
            default -> now.plusSeconds(7L * 24 * 60 * 60);
        };
    }


    private void createAutopilotTimelineEvent(
            AutopilotDispatchQueue queue,
            CrmTimelineEventType eventType,
            String title,
            String description
    ) {
        if (queue == null || queue.getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(queue.getCrmCase());
        timeline.setEventType(eventType);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setActorName(employee != null ? employee.getFullName() : "System");

        crmCaseTimelineRepository.save(timeline);
    }

    private void createAutopilotAuditEvent(
            AuditAction action,
            UUID entityId,
            String description
    ) {
        auditLogService.record(
                action,
                AuditEntityType.USER_PROFILE,
                entityId,
                description
        );
    }


}