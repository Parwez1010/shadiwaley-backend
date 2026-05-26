package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.chat.domain.ChatFamilyDecision;
import com.shadiwaley.server.chat.domain.ChatModerationStatus;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.dto.admin.request.*;
import com.shadiwaley.server.chat.dto.admin.response.*;
import com.shadiwaley.server.chat.infrastructure.entity.*;
import com.shadiwaley.server.chat.infrastructure.repository.*;
import com.shadiwaley.server.crm.domain.*;
import com.shadiwaley.server.crm.infrastructure.entity.*;
import com.shadiwaley.server.crm.infrastructure.repository.*;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.media.domain.MediaType;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.domain.ProposalStatus;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.rishtapipeline.application.service.RishtaPipelineStageResolver;
import com.shadiwaley.server.rishtapipeline.domain.RishtaPipelineStage;
import com.shadiwaley.server.rishtapipeline.infrastructure.entity.RishtaPipelineNote;
import com.shadiwaley.server.rishtapipeline.infrastructure.repository.RishtaPipelineNoteRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

import static com.shadiwaley.server.autopilot.domain.AutopilotResponseStatus.FOLLOW_UP_REQUIRED;
import static com.shadiwaley.server.chat.domain.ChatFamilyDecision.MEETING_SCHEDULED;
import static com.shadiwaley.server.chat.domain.ChatRoomStatus.CLOSED_NO_RESPONSE;
import static com.shadiwaley.server.proposal.domain.ProposalStatus.INTERESTED;

@Service
@RequiredArgsConstructor
public class AdminChatMonitorService {

    private final FamilyChatRoomRepository chatRoomRepository;
    private final FamilyChatMessageRepository chatMessageRepository;
    private final ChatMonitorNoteRepository chatMonitorNoteRepository;
    private final ChatFamilyDecisionLogRepository chatFamilyDecisionLogRepository;

    private final EmployeeAccountRepository employeeAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final ProposalRepository proposalRepository;
    private final CrmFollowUpRepository crmFollowUpRepository;
    private final CrmCaseTimelineRepository crmCaseTimelineRepository;

    private final AdminChatMonitorPermissionService permissionService;
    private final RishtaPipelineStageResolver stageResolver;

    private final MediaFileRepository mediaFileRepository;
    private final RishtaPipelineNoteRepository rishtaPipelineNoteRepository;

    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AdminChatMonitorSummaryResponse getSummary(
            LocalDate fromDate,
            LocalDate toDate,
            UUID assignedEmployeeId
    ) {
        EmployeeAccount employee = permissionService.getCurrentEmployee();
        UUID effectiveEmployeeId = resolveEffectiveEmployeeId(employee, assignedEmployeeId);

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Specification<FamilyChatRoom> spec = summarySpecification(effectiveEmployeeId, from, to);

        List<FamilyChatRoom> rooms = chatRoomRepository.findAll(spec);

        long totalRooms = rooms.size();
        long activeRooms = rooms.stream().filter(r -> r.getStatus() == ChatRoomStatus.ACTIVE).count();
        long needsAttention = rooms.stream().filter(FamilyChatRoom::isNeedsAttention).count();
        long reportedRooms = rooms.stream().filter(FamilyChatRoom::isReported).count();
        long blockedRooms = rooms.stream().filter(FamilyChatRoom::isBlocked).count();
        long closedSuccess = rooms.stream().filter(r -> r.getStatus() == ChatRoomStatus.CLOSED_SUCCESS).count();
        long closedRejected = rooms.stream().filter(r -> r.getStatus() == ChatRoomStatus.CLOSED_REJECTED).count();
        long pendingResponse = rooms.stream().filter(r -> r.getStatus() == ChatRoomStatus.PENDING_RESPONSE).count();

        long todayMessages = chatMessageRepository.countBySentAtBetween(
                LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant(),
                LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        );

        return AdminChatMonitorSummaryResponse.builder()
                .totalRooms(totalRooms)
                .activeRooms(activeRooms)
                .needsAttention(needsAttention)
                .reportedRooms(reportedRooms)
                .blockedRooms(blockedRooms)
                .closedSuccess(closedSuccess)
                .closedRejected(closedRejected)
                .pendingResponse(pendingResponse)
                .unreadMessages(0)
                .todayMessages(todayMessages)
                .avgResponseMinutes(0)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChatRoomPageResponse getRooms(
            int page,
            int size,
            String search,
            ChatRoomStatus status,
            UUID assignedEmployeeId,
            UUID crmCaseId,
            UUID proposalId,
            UUID pipelineId,
            UUID fromProfileId,
            UUID toProfileId,
            String district,
            String side,
            Boolean hasUnread,
            Boolean reportedOnly,
            Boolean needsAttentionOnly,
            LocalDate fromDate,
            LocalDate toDate,
            String sort
    ) {
        EmployeeAccount employee = permissionService.getCurrentEmployee();
        UUID effectiveEmployeeId = resolveEffectiveEmployeeId(employee, assignedEmployeeId);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                resolveSort(sort)
        );

        Instant from = fromDate != null
                ? fromDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Instant to = toDate != null
                ? toDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                : null;

        Page<FamilyChatRoom> result = chatRoomRepository.findAll(
                roomSpecification(
                        search,
                        status,
                        effectiveEmployeeId,
                        crmCaseId,
                        proposalId,
                        pipelineId,
                        fromProfileId,
                        toProfileId,
                        district,
                        side,
                        reportedOnly,
                        needsAttentionOnly,
                        from,
                        to
                ),
                pageable
        );

        List<AdminChatRoomListItemResponse> items = result.getContent()
                .stream()
                .map(this::toRoomListItem)
                .toList();

        return AdminChatRoomPageResponse.builder()
                .items(items)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private UUID resolveEffectiveEmployeeId(EmployeeAccount employee, UUID requestedEmployeeId) {
        if (permissionService.isAdmin(employee)) {
            return requestedEmployeeId;
        }

        return employee.getId();
    }

    private Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "lastMessageAt");
        }

        String normalized = sort.trim().toLowerCase();

        if (normalized.contains("createdat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "createdAt")
                    : Sort.by(Sort.Direction.DESC, "createdAt");
        }

        if (normalized.contains("updatedat")) {
            return normalized.endsWith("asc")
                    ? Sort.by(Sort.Direction.ASC, "updatedAt")
                    : Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        return normalized.endsWith("asc")
                ? Sort.by(Sort.Direction.ASC, "lastMessageAt")
                : Sort.by(Sort.Direction.DESC, "lastMessageAt");
    }

    private Specification<FamilyChatRoom> summarySpecification(
            UUID assignedEmployeeId,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (assignedEmployeeId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("assignedEmployee").get("id"), assignedEmployeeId));
            }

            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), to));
            }

            return predicate;
        };
    }

    private Specification<FamilyChatRoom> roomSpecification(
            String search,
            ChatRoomStatus status,
            UUID assignedEmployeeId,
            UUID crmCaseId,
            UUID proposalId,
            UUID pipelineId,
            UUID fromProfileId,
            UUID toProfileId,
            String district,
            String side,
            Boolean reportedOnly,
            Boolean needsAttentionOnly,
            Instant from,
            Instant to
    ) {
        return (root, query, cb) -> {
            query.distinct(true);

            Predicate predicate = cb.conjunction();

            Join<FamilyChatRoom, Proposal> proposalJoin = root.join("proposal", JoinType.LEFT);

            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }

            if (assignedEmployeeId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("assignedEmployee").get("id"), assignedEmployeeId));
            }

            if (crmCaseId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("crmCase").get("id"), crmCaseId));
            }

            if (proposalId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("proposal").get("id"), proposalId));
            }

            if (pipelineId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("proposal").get("id"), pipelineId));
            }

            if (fromProfileId != null) {
                predicate = cb.and(predicate, cb.equal(proposalJoin.get("fromProfile").get("id"), fromProfileId));
            }

            if (toProfileId != null) {
                predicate = cb.and(predicate, cb.equal(proposalJoin.get("toProfile").get("id"), toProfileId));
            }

            if (Boolean.TRUE.equals(reportedOnly)) {
                predicate = cb.and(predicate, cb.isTrue(root.get("reported")));
            }

            if (Boolean.TRUE.equals(needsAttentionOnly)) {
                predicate = cb.and(predicate, cb.isTrue(root.get("needsAttention")));
            }

            if (from != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }

            if (to != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("createdAt"), to));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicate = cb.and(predicate,
                        cb.or(
                                cb.like(cb.lower(root.get("id").as(String.class)), pattern),
                                cb.like(cb.lower(root.get("lastMessageText")), pattern),
                                cb.like(cb.lower(proposalJoin.get("id").as(String.class)), pattern)
                        )
                );
            }

            return predicate;
        };
    }

    private AdminChatRoomListItemResponse toRoomListItem(FamilyChatRoom room) {
        Proposal proposal = room.getProposal();

        UserProfile fromProfile = proposal != null ? proposal.getFromProfile() : null;
        UserProfile toProfile = proposal != null ? proposal.getToProfile() : null;

        ParentProfile fromParent = fromProfile != null && fromProfile.getUserAccount() != null
                ? parentProfileRepository.findByUserAccountId(fromProfile.getUserAccount().getId()).orElse(null)
                : null;

        ParentProfile toParent = toProfile != null && toProfile.getUserAccount() != null
                ? parentProfileRepository.findByUserAccountId(toProfile.getUserAccount().getId()).orElse(null)
                : null;

        RishtaPipelineStage pipelineStage = proposal != null
                ? stageResolver.resolve(proposal.getStatus())
                : null;

        return AdminChatRoomListItemResponse.builder()
                .roomId(room.getId())
                .proposalId(proposal != null ? proposal.getId() : null)
                .pipelineId(proposal != null ? proposal.getId() : null)
                .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)

                .fromProfileId(fromProfile != null ? fromProfile.getId() : null)
                .fromCandidateName(fromProfile != null ? fromProfile.getCandidateFirstName() : null)
                .fromParentName(fromParent != null ? fromParent.getParentName() : null)
                .fromPhone(fromProfile != null && fromProfile.getUserAccount() != null ? fromProfile.getUserAccount().getPhone() : null)
                .fromSide(fromProfile != null && fromProfile.getUserAccount() != null && fromProfile.getUserAccount().getSide() != null ? fromProfile.getUserAccount().getSide().name() : null)
                .fromDistrict(fromParent != null ? fromParent.getDistrict() : null)

                .toProfileId(toProfile != null ? toProfile.getId() : null)
                .toCandidateName(toProfile != null ? toProfile.getCandidateFirstName() : null)
                .toParentName(toParent != null ? toParent.getParentName() : null)
                .toPhone(toProfile != null && toProfile.getUserAccount() != null ? toProfile.getUserAccount().getPhone() : null)
                .toSide(toProfile != null && toProfile.getUserAccount() != null && toProfile.getUserAccount().getSide() != null ? toProfile.getUserAccount().getSide().name() : null)
                .toDistrict(toParent != null ? toParent.getDistrict() : null)

                .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getFullName() : null)

                .status(room.getStatus())
                .statusLabel(statusLabel(room.getStatus()))

                .lastMessageText(room.getLastMessageText())
                .lastMessageType(room.getLastMessageType())
                .lastMessageAt(room.getLastMessageAt())
                .lastMessageByName(room.getLastMessageByName())

                .unreadCount(0)
                .messageCount(room.getMessageCount())
                .reported(room.isReported())
                .needsAttention(room.isNeedsAttention())
                .blocked(room.isBlocked())

                .matchScore(proposal != null ? proposal.getMatchScore() : null)
                .proposalStatus(proposal != null ? proposal.getStatus() : null)
                .pipelineStage(pipelineStage)
                .pipelineStageLabel(pipelineStage != null ? stageResolver.label(pipelineStage) : null)

                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private String statusLabel(ChatRoomStatus status) {
        if (status == null) return "Unknown";

        return switch (status) {
            case ACTIVE -> "Active";
            case PENDING_RESPONSE -> "Pending Response";
            case NEEDS_CRM_ATTENTION -> "Needs CRM Attention";
            case REPORTED -> "Reported";
            case BLOCKED -> "Blocked";
            case CLOSED_SUCCESS -> "Closed Success";
            case CLOSED_REJECTED -> "Closed Rejected";
            case CLOSED_NO_RESPONSE -> "Closed No Response";
            case CLOSED_BY_ADMIN -> "Closed By Admin";
            case CLOSED -> "Closed";
        };
    }

    @Transactional(readOnly = true)
    public AdminChatRoomDetailResponse getRoomDetail(UUID roomId) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanViewRoom(room);

        Proposal proposal = room.getProposal();

        UserProfile fromProfile = proposal != null ? proposal.getFromProfile() : null;
        UserProfile toProfile = proposal != null ? proposal.getToProfile() : null;

        ParentProfile fromParent = resolveParent(fromProfile);
        ParentProfile toParent = resolveParent(toProfile);

        RishtaPipelineStage pipelineStage = proposal != null
                ? stageResolver.resolve(proposal.getStatus())
                : null;

        CrmFollowUp latestFollowUp = proposal != null
                ? crmFollowUpRepository.findTopByProposal_IdOrderByScheduledAtDesc(proposal.getId()).orElse(null)
                : null;

        RishtaPipelineNote latestNote = proposal != null
                ? rishtaPipelineNoteRepository.findTopByProposalIdOrderByCreatedAtDesc(proposal.getId()).orElse(null)
                : null;

        long attachmentCount = chatMessageRepository.countByRoomIdAndMediaFileIsNotNull(room.getId());

        return AdminChatRoomDetailResponse.builder()
                .room(AdminChatRoomDetailResponse.RoomInfo.builder()
                        .roomId(room.getId())
                        .proposalId(proposal != null ? proposal.getId() : null)
                        .pipelineId(proposal != null ? proposal.getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .status(room.getStatus())
                        .statusLabel(statusLabel(room.getStatus()))
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .assignedEmployeeName(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getFullName() : null)
                        .matchScore(proposal != null ? proposal.getMatchScore() : null)
                        .proposalStatus(proposal != null ? proposal.getStatus() : null)
                        .pipelineStage(pipelineStage)
                        .pipelineStageLabel(pipelineStage != null ? stageResolver.label(pipelineStage) : null)
                        .createdAt(room.getCreatedAt())
                        .lastMessageAt(room.getLastMessageAt())
                        .build())
                .fromProfile(toProfileInfo(fromProfile, fromParent))
                .toProfile(toProfileInfo(toProfile, toParent))
                .proposal(proposal != null
                        ? AdminChatRoomDetailResponse.ProposalInfo.builder()
                        .proposalId(proposal.getId())
                        .status(proposal.getStatus())
                        .matchScore(proposal.getMatchScore())
                        .sentAt(proposal.getDispatchedAt())
                        .build()
                        : null)
                .pipeline(AdminChatRoomDetailResponse.PipelineInfo.builder()
                        .pipelineStage(pipelineStage)
                        .pipelineStageLabel(pipelineStage != null ? stageResolver.label(pipelineStage) : null)
                        .nextFollowUpAt(latestFollowUp != null ? latestFollowUp.getScheduledAt() : null)
                        .lastNote(latestNote != null ? latestNote.getNote() : null)
                        .build())
                .moderation(AdminChatRoomDetailResponse.ModerationInfo.builder()
                        .reported(room.isReported())
                        .blocked(room.isBlocked())
                        .needsAttention(room.isNeedsAttention())
                        .lastReportReason(room.getLastReportReason())
                        .build())
                .stats(AdminChatRoomDetailResponse.StatsInfo.builder()
                        .messageCount(room.getMessageCount())
                        .unreadCount(0)
                        .attachmentCount(attachmentCount)
                        .lastMessageAt(room.getLastMessageAt())
                        .build())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChatMessagePageResponse getMessages(
            UUID roomId,
            int page,
            int size,
            UUID beforeMessageId,
            UUID afterMessageId,
            String sort
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanViewRoom(room);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                resolveMessageSort(sort)
        );

        Page<FamilyChatMessage> result;

        if (beforeMessageId != null) {
            FamilyChatMessage cursor = getMessageOrThrow(beforeMessageId);

            result = chatMessageRepository.findByRoomIdAndSentAtBeforeOrderBySentAtDesc(
                    roomId,
                    cursor.getSentAt(),
                    pageable
            );
        } else if (afterMessageId != null) {
            FamilyChatMessage cursor = getMessageOrThrow(afterMessageId);

            result = chatMessageRepository.findByRoomIdAndSentAtAfterOrderBySentAtAsc(
                    roomId,
                    cursor.getSentAt(),
                    pageable
            );
        } else {
            result = chatMessageRepository.findByRoomIdOrderBySentAtDesc(roomId, pageable);
        }

        return AdminChatMessagePageResponse.builder()
                .items(result.getContent().stream().map(this::toAdminMessage).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private FamilyChatRoom getRoomOrThrow(UUID roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));
    }

    private FamilyChatMessage getMessageOrThrow(UUID messageId) {
        return chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Chat message not found"));
    }

    private ParentProfile resolveParent(UserProfile profile) {
        if (profile == null || profile.getUserAccount() == null) {
            return null;
        }

        return parentProfileRepository
                .findByUserAccountId(profile.getUserAccount().getId())
                .orElse(null);
    }

    private AdminChatRoomDetailResponse.ProfileInfo toProfileInfo(
            UserProfile profile,
            ParentProfile parent
    ) {
        if (profile == null) {
            return null;
        }

        String photoUrl = null;

        MediaFile primaryPhoto = mediaFileRepository
                .findTopByUserProfileIdAndMediaTypeAndPrimaryTrueAndDeletedFalseOrderByCreatedAtDesc(
                        profile.getId(),
                        MediaType.PROFILE_PHOTO
                )
                .orElse(null);

        if (primaryPhoto != null) {
            photoUrl = "/api/v1/media/" + primaryPhoto.getId() + "/view";
        }

        return AdminChatRoomDetailResponse.ProfileInfo.builder()
                .profileId(profile.getId())
                .candidateName(profile.getCandidateFirstName())
                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : null)
                .side(profile.getUserAccount() != null && profile.getUserAccount().getSide() != null
                        ? profile.getUserAccount().getSide().name()
                        : null)
                .age(profile.getCandidateAge())
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .education(profile.getEducation())
                .professionTitle(profile.getProfessionTitle())
                .profilePhotoViewUrl(photoUrl)
                .build();
    }

    private AdminChatMessageResponse toAdminMessage(FamilyChatMessage message) {
        UserProfile senderProfile = userProfileRepository
                .findByUserAccountId(message.getSenderUser().getId())
                .orElse(null);

        String senderSide = message.getSenderUser().getSide() != null
                ? message.getSenderUser().getSide().name()
                : null;

        String direction = resolveDirection(message);

        MediaFile media = message.getMediaFile();

        return AdminChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .senderUserId(message.getSenderUser().getId())
                .senderProfileId(senderProfile != null ? senderProfile.getId() : null)
                .senderName(senderProfile != null ? senderProfile.getCandidateFirstName() : "Family")
                .senderRole("PARENT")
                .senderSide(senderSide)
                .messageType(message.getMessageType())
                .text(message.getContent())
                .attachmentUrl(media != null ? "/api/v1/media/" + media.getId() + "/view" : null)
                .attachmentFileName(media != null ? media.getOriginalFileName() : null)
                .attachmentMimeType(media != null ? media.getContentType() : null)
                .attachmentSizeBytes(media != null ? media.getFileSizeBytes() : null)
                .direction(direction)
                .moderationStatus(message.getModerationStatus())
                .reported(message.getModerationStatus() == com.shadiwaley.server.chat.domain.ChatModerationStatus.REPORTED
                        || message.getModerationStatus() == com.shadiwaley.server.chat.domain.ChatModerationStatus.UNDER_REVIEW)
                .hidden(message.getModerationStatus() == com.shadiwaley.server.chat.domain.ChatModerationStatus.HIDDEN)
                .deleted(message.getDeletedAt() != null
                        || message.getModerationStatus() == com.shadiwaley.server.chat.domain.ChatModerationStatus.DELETED)
                .readByOtherSide(message.getReadAt() != null)
                .readAt(message.getReadAt())
                .createdAt(message.getSentAt())
                .updatedAt(message.getEditedAt() != null ? message.getEditedAt() : message.getSentAt())
                .build();
    }

    private String resolveDirection(FamilyChatMessage message) {
        FamilyChatRoom room = message.getRoom();

        if (room.getBoyUser() != null
                && message.getSenderUser().getId().equals(room.getBoyUser().getId())) {
            return "FAMILY_A_TO_FAMILY_B";
        }

        if (room.getGirlUser() != null
                && message.getSenderUser().getId().equals(room.getGirlUser().getId())) {
            return "FAMILY_B_TO_FAMILY_A";
        }

        return "SYSTEM";
    }

    private Sort resolveMessageSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "sentAt");
        }

        String normalized = sort.trim().toLowerCase();

        return normalized.endsWith("asc")
                ? Sort.by(Sort.Direction.ASC, "sentAt")
                : Sort.by(Sort.Direction.DESC, "sentAt");
    }

    @Transactional
    public AdminChatNoteResponse addNote(UUID roomId, AdminChatNoteRequest request) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        ChatMonitorNote note = new ChatMonitorNote();
        note.setRoom(room);
        note.setNote(request.getNote().trim());
        note.setCreatedByEmployee(employee);
        note.setCreatedByName(employee.getFullName());

        ChatMonitorNote saved = chatMonitorNoteRepository.save(note);

        addChatTimeline(
                room,
                CrmTimelineEventType.CHAT_INTERNAL_NOTE_ADDED,
                "Chat note added",
                saved.getNote(),
                null,
                null
        );

        auditLogService.record(
                AuditAction.CHAT_INTERNAL_NOTE_ADDED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Internal chat note added"
        );

        return toNoteResponse(saved);
    }

    @Transactional
    public AdminChatStatusUpdateResponse updateStatus(
            UUID roomId,
            AdminChatStatusUpdateRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        ChatRoomStatus oldStatus = room.getStatus();

        room.setStatus(request.getStatus());
        room.setNeedsAttention(request.getStatus() == ChatRoomStatus.NEEDS_CRM_ATTENTION);
        room.setBlocked(request.getStatus() == ChatRoomStatus.BLOCKED);

        if (isClosedStatus(request.getStatus())) {
            room.setClosedAt(Instant.now());
        }

        FamilyChatRoom saved = chatRoomRepository.save(room);

        if (request.getNote() != null && !request.getNote().isBlank()) {
            AdminChatNoteRequest noteRequest = new AdminChatNoteRequest();
            noteRequest.setNote(request.getNote());
            addNote(roomId, noteRequest);
        }

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ROOM_STATUS_UPDATED,
                "Chat status updated",
                "Chat status changed to " + request.getStatus(),
                oldStatus != null ? oldStatus.name() : null,
                request.getStatus().name()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_STATUS_UPDATED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room status changed from "
                        + (oldStatus != null ? oldStatus.name() : "NONE")
                        + " to "
                        + request.getStatus().name()
        );

        return AdminChatStatusUpdateResponse.builder()
                .roomId(saved.getId())
                .status(saved.getStatus())
                .statusLabel(statusLabel(saved.getStatus()))
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Transactional
    public AdminMessageModerationResponse moderateMessage(
            UUID messageId,
            AdminMessageModerationRequest request
    ) {
        FamilyChatMessage message = getMessageOrThrow(messageId);
        FamilyChatRoom room = message.getRoom();

        permissionService.assertCanModerateMessage(room);

        message.setModerationStatus(request.getModerationStatus());

        if (request.getModerationStatus() == ChatModerationStatus.DELETED) {
            message.setDeletedAt(Instant.now());
            message.setContent("This message was removed by admin.");
        }

        if (request.getModerationStatus() == ChatModerationStatus.HIDDEN
                || request.getModerationStatus() == ChatModerationStatus.FLAGGED
                || request.getModerationStatus() == ChatModerationStatus.UNDER_REVIEW
                || request.getModerationStatus() == ChatModerationStatus.REPORTED) {
            room.setNeedsAttention(true);
            room.setStatus(ChatRoomStatus.NEEDS_CRM_ATTENTION);
        }

        FamilyChatMessage saved = chatMessageRepository.save(message);
        chatRoomRepository.save(room);

        addChatTimeline(
                room,
                CrmTimelineEventType.CHAT_MESSAGE_MODERATED,
                "Chat message moderated",
                request.getReason() != null && !request.getReason().isBlank()
                        ? request.getReason()
                        : "Message moderation changed to " + request.getModerationStatus(),
                null,
                request.getModerationStatus().name()
        );

        auditLogService.record(
                AuditAction.CHAT_MESSAGE_MODERATED,
                AuditEntityType.CHAT_MESSAGE,
                saved.getId(),
                "Message moderation updated to " + request.getModerationStatus()
        );

        return AdminMessageModerationResponse.builder()
                .messageId(saved.getId())
                .moderationStatus(saved.getModerationStatus())
                .updatedAt(Instant.now())
                .build();
    }

    @Transactional
    public AdminChatStatusUpdateResponse reportRoom(
            UUID roomId,
            AdminChatActionReasonRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        room.setReported(true);
        room.setNeedsAttention(true);
        room.setLastReportReason(request.getReason());
        room.setStatus(ChatRoomStatus.REPORTED);

        FamilyChatRoom saved = chatRoomRepository.save(room);

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ROOM_REPORTED,
                "Chat room reported",
                request.getReason(),
                null,
                ChatRoomStatus.REPORTED.name()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_REPORTED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room reported: " + request.getReason()
        );

        return statusResponse(saved);
    }

    @Transactional
    public AdminChatStatusUpdateResponse blockRoom(
            UUID roomId,
            AdminChatActionReasonRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        room.setBlocked(true);
        room.setBlockedByUser(null);
        room.setNeedsAttention(true);
        room.setStatus(ChatRoomStatus.BLOCKED);
        room.setLastReportReason(request.getReason());

        FamilyChatRoom saved = chatRoomRepository.save(room);

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ROOM_BLOCKED,
                "Chat room blocked",
                request.getReason(),
                null,
                ChatRoomStatus.BLOCKED.name()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_BLOCKED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room blocked by " + employee.getFullName()
        );

        return statusResponse(saved);
    }

    @Transactional
    public AdminChatStatusUpdateResponse unblockRoom(
            UUID roomId,
            AdminChatActionReasonRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        ChatRoomStatus oldStatus = room.getStatus();

        room.setBlocked(false);
        room.setStatus(ChatRoomStatus.ACTIVE);
        room.setNeedsAttention(false);

        FamilyChatRoom saved = chatRoomRepository.save(room);

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ROOM_UNBLOCKED,
                "Chat room unblocked",
                request.getReason(),
                oldStatus != null ? oldStatus.name() : null,
                ChatRoomStatus.ACTIVE.name()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_UNBLOCKED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room unblocked"
        );

        return statusResponse(saved);
    }

    @Transactional
    public AdminChatStatusUpdateResponse closeRoom(
            UUID roomId,
            AdminChatCloseRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        if (!isClosedStatus(request.getStatus())) {
            throw new IllegalArgumentException("Only closed status is allowed");
        }

        ChatRoomStatus oldStatus = room.getStatus();

        room.setStatus(request.getStatus());
        room.setClosedAt(Instant.now());
        room.setNeedsAttention(false);

        FamilyChatRoom saved = chatRoomRepository.save(room);

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ROOM_CLOSED,
                "Chat room closed",
                request.getReason(),
                oldStatus != null ? oldStatus.name() : null,
                request.getStatus().name()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_CLOSED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room closed with status " + request.getStatus()
        );

        return statusResponse(saved);
    }

    private AdminChatNoteResponse toNoteResponse(ChatMonitorNote note) {
        return AdminChatNoteResponse.builder()
                .noteId(note.getId())
                .roomId(note.getRoom().getId())
                .note(note.getNote())
                .createdByEmployeeId(note.getCreatedByEmployee() != null ? note.getCreatedByEmployee().getId() : null)
                .createdByName(note.getCreatedByName())
                .createdAt(note.getCreatedAt())
                .build();
    }

    private AdminChatStatusUpdateResponse statusResponse(FamilyChatRoom room) {
        return AdminChatStatusUpdateResponse.builder()
                .roomId(room.getId())
                .status(room.getStatus())
                .statusLabel(statusLabel(room.getStatus()))
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    private boolean isClosedStatus(ChatRoomStatus status) {
        return status == ChatRoomStatus.CLOSED
                || status == ChatRoomStatus.CLOSED_SUCCESS
                || status == ChatRoomStatus.CLOSED_REJECTED
                || status == CLOSED_NO_RESPONSE
                || status == ChatRoomStatus.CLOSED_BY_ADMIN;
    }

    private void addChatTimeline(
            FamilyChatRoom room,
            CrmTimelineEventType eventType,
            String title,
            String description,
            String oldValue,
            String newValue
    ) {
        if (room.getCrmCase() == null) {
            return;
        }

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        CrmCaseTimeline timeline = new CrmCaseTimeline();
        timeline.setCrmCase(room.getCrmCase());
        timeline.setEventType(eventType);
        timeline.setTitle(title);
        timeline.setDescription(description);
        timeline.setActorEmployee(employee);
        timeline.setActorName(employee != null ? employee.getFullName() : "System");
        timeline.setOldValue(oldValue);
        timeline.setNewValue(newValue);
        timeline.setMetadata("{\"roomId\":\"" + room.getId() + "\"}");

        crmCaseTimelineRepository.save(timeline);
    }

    @Transactional
    public AdminChatAssignmentResponse updateAssignment(
            UUID roomId,
            AdminChatAssignmentRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);

        permissionService.assertCanAssignRoom();

        EmployeeAccount employee = employeeAccountRepository.findById(request.getAssignedEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Assigned employee not found"));

        String oldAssignee = room.getAssignedEmployee() != null
                ? room.getAssignedEmployee().getFullName()
                : null;

        room.setAssignedEmployee(employee);

        if (room.getCrmCase() != null) {
            room.getCrmCase().setAssignedEmployee(employee);
        }

        FamilyChatRoom saved = chatRoomRepository.save(room);

        if (request.getNote() != null && !request.getNote().isBlank()) {
            AdminChatNoteRequest noteRequest = new AdminChatNoteRequest();
            noteRequest.setNote(request.getNote());
            addNote(roomId, noteRequest);
        }

        addChatTimeline(
                saved,
                CrmTimelineEventType.CHAT_ASSIGNMENT_UPDATED,
                "Chat assignment updated",
                "Chat assigned to " + employee.getFullName(),
                oldAssignee,
                employee.getFullName()
        );

        auditLogService.record(
                AuditAction.CHAT_ASSIGNMENT_UPDATED,
                AuditEntityType.CHAT_ROOM,
                saved.getId(),
                "Chat room assigned to " + employee.getFullName()
        );

        return AdminChatAssignmentResponse.builder()
                .roomId(saved.getId())
                .assignedEmployeeId(employee.getId())
                .assignedEmployeeName(employee.getFullName())
                .build();
    }

    @Transactional
    public AdminChatDecisionResponse recordDecision(
            UUID roomId,
            AdminChatDecisionRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        EmployeeAccount employee = permissionService.getCurrentEmployee();

        ChatFamilyDecisionLog decisionLog = new ChatFamilyDecisionLog();
        decisionLog.setRoom(room);
        decisionLog.setDecision(request.getDecision());
        decisionLog.setNote(request.getNote());
        decisionLog.setNextFollowUpAt(request.getNextFollowUpAt());
        decisionLog.setCreatedByEmployee(employee);
        decisionLog.setCreatedByName(employee.getFullName());

        chatFamilyDecisionLogRepository.save(decisionLog);

        Proposal proposal = room.getProposal();

        if (proposal != null) {
            ProposalStatus newStatus = resolveProposalStatusFromDecision(request.getDecision());

            if (newStatus != null) {
                proposal.setStatus(newStatus);
                proposal.setLastUpdatedByEmployee(employee);
                proposal.setLastUpdatedByName(employee.getFullName());
                proposal.setLastUpdatedAt(Instant.now());
                proposalRepository.save(proposal);
            }
        }

        if (request.getNextFollowUpAt() != null) {
            createFollowUpInternal(
                    room,
                    request.getNextFollowUpAt(),
                    CrmFollowUpChannel.PHONE,
                    request.getNote() != null && !request.getNote().isBlank()
                            ? request.getNote()
                            : "Chat decision follow-up"
            );
        }

        updateRoomStatusFromDecision(room, request.getDecision());
        FamilyChatRoom savedRoom = chatRoomRepository.save(room);

        RishtaPipelineStage pipelineStage = proposal != null
                ? stageResolver.resolve(proposal.getStatus())
                : null;

        addChatTimeline(
                savedRoom,
                CrmTimelineEventType.CHAT_DECISION_RECORDED,
                "Family decision recorded",
                request.getDecision().name(),
                null,
                request.getDecision().name()
        );

        auditLogService.record(
                AuditAction.CHAT_DECISION_RECORDED,
                AuditEntityType.CHAT_ROOM,
                savedRoom.getId(),
                "Family decision recorded: " + request.getDecision()
        );

        return AdminChatDecisionResponse.builder()
                .roomId(savedRoom.getId())
                .decision(request.getDecision())
                .proposalStatus(proposal != null ? proposal.getStatus() : null)
                .pipelineStage(pipelineStage)
                .nextFollowUpAt(request.getNextFollowUpAt())
                .build();
    }

    @Transactional
    public AdminChatFollowUpResponse scheduleFollowUp(
            UUID roomId,
            AdminChatFollowUpRequest request
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanUpdateRoom(room);

        CrmFollowUp saved = createFollowUpInternal(
                room,
                request.getScheduledAt(),
                request.getChannel(),
                request.getPurpose()
        );

        addChatTimeline(
                room,
                CrmTimelineEventType.CHAT_FOLLOW_UP_SCHEDULED,
                "Chat follow-up scheduled",
                request.getPurpose(),
                null,
                request.getScheduledAt().toString()
        );

        auditLogService.record(
                AuditAction.CHAT_FOLLOW_UP_SCHEDULED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat follow-up scheduled"
        );

        return AdminChatFollowUpResponse.builder()
                .followUpId(saved.getId())
                .roomId(room.getId())
                .scheduledAt(saved.getScheduledAt())
                .channel(saved.getChannel())
                .purpose(saved.getPurpose())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChatHistoryPageResponse<AdminChatNoteResponse> getNotes(
            UUID roomId,
            int page,
            int size
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanViewRoom(room);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        Page<ChatMonitorNote> result =
                chatMonitorNoteRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        return AdminChatHistoryPageResponse.<AdminChatNoteResponse>builder()
                .items(result.getContent().stream().map(this::toNoteResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChatHistoryPageResponse<AdminChatDecisionLogResponse> getDecisions(
            UUID roomId,
            int page,
            int size
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanViewRoom(room);

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100)
        );

        Page<ChatFamilyDecisionLog> result =
                chatFamilyDecisionLogRepository.findByRoomIdOrderByCreatedAtDesc(roomId, pageable);

        return AdminChatHistoryPageResponse.<AdminChatDecisionLogResponse>builder()
                .items(result.getContent().stream().map(this::toDecisionLogResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public AdminChatHistoryPageResponse<AdminChatFollowUpResponse> getFollowUps(
            UUID roomId,
            int page,
            int size
    ) {
        FamilyChatRoom room = getRoomOrThrow(roomId);
        permissionService.assertCanViewRoom(room);

        if (room.getProposal() == null) {
            return AdminChatHistoryPageResponse.<AdminChatFollowUpResponse>builder()
                    .items(List.of())
                    .page(0)
                    .size(size)
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        List<AdminChatFollowUpResponse> items =
                crmFollowUpRepository.findByProposal_IdOrderByScheduledAtDesc(room.getProposal().getId())
                        .stream()
                        .skip((long) Math.max(page, 0) * Math.min(Math.max(size, 1), 100))
                        .limit(Math.min(Math.max(size, 1), 100))
                        .map(followUp -> AdminChatFollowUpResponse.builder()
                                .followUpId(followUp.getId())
                                .roomId(room.getId())
                                .scheduledAt(followUp.getScheduledAt())
                                .channel(followUp.getChannel())
                                .purpose(followUp.getPurpose())
                                .build())
                        .toList();

        long total = crmFollowUpRepository.findByProposal_IdOrderByScheduledAtDesc(room.getProposal().getId()).size();
        int safeSize = Math.min(Math.max(size, 1), 100);

        return AdminChatHistoryPageResponse.<AdminChatFollowUpResponse>builder()
                .items(items)
                .page(Math.max(page, 0))
                .size(safeSize)
                .totalElements(total)
                .totalPages(total == 0 ? 0 : (int) Math.ceil((double) total / safeSize))
                .last(((long) (Math.max(page, 0) + 1) * safeSize) >= total)
                .build();
    }

    private CrmFollowUp createFollowUpInternal(
            FamilyChatRoom room,
            Instant scheduledAt,
            CrmFollowUpChannel channel,
            String purpose
    ) {
        if (room.getCrmCase() == null) {
            throw new IllegalStateException("Chat room is not linked with a CRM case");
        }

        EmployeeAccount employee = room.getAssignedEmployee() != null
                ? room.getAssignedEmployee()
                : permissionService.getCurrentEmployee();

        CrmFollowUp followUp = new CrmFollowUp();
        followUp.setCrmCase(room.getCrmCase());
        followUp.setProposal(room.getProposal());
        followUp.setAssignedEmployee(employee);
        followUp.setScheduledAt(scheduledAt);
        followUp.setStatus(CrmFollowUpStatus.SCHEDULED);
        followUp.setChannel(channel);
        followUp.setPurpose(purpose.trim());

        room.getCrmCase().setNextFollowUpAt(scheduledAt);

        return crmFollowUpRepository.save(followUp);
    }

    private ProposalStatus resolveProposalStatusFromDecision(ChatFamilyDecision decision) {
        return switch (decision) {
            case INTERESTED -> INTERESTED;
            case NOT_INTERESTED -> ProposalStatus.NOT_INTERESTED;
            case MEETING_SCHEDULED -> ProposalStatus.MEETING_DISCUSSION;
            case ACCEPTED, ENGAGED -> ProposalStatus.ACCEPTED;
            case REJECTED -> ProposalStatus.REJECTED;
            case CLOSED_NO_RESPONSE -> ProposalStatus.EXPIRED;
            case FOLLOW_UP_REQUIRED -> null;
        };
    }

    private void updateRoomStatusFromDecision(
            FamilyChatRoom room,
            ChatFamilyDecision decision
    ) {
        switch (decision) {
            case ACCEPTED, ENGAGED -> {
                room.setStatus(ChatRoomStatus.CLOSED_SUCCESS);
                room.setClosedAt(Instant.now());
                room.setNeedsAttention(false);
            }
            case REJECTED, NOT_INTERESTED -> {
                room.setStatus(ChatRoomStatus.CLOSED_REJECTED);
                room.setClosedAt(Instant.now());
                room.setNeedsAttention(false);
            }
            case CLOSED_NO_RESPONSE -> {
                room.setStatus(CLOSED_NO_RESPONSE);
                room.setClosedAt(Instant.now());
                room.setNeedsAttention(false);
            }
            case FOLLOW_UP_REQUIRED, MEETING_SCHEDULED -> {
                room.setStatus(ChatRoomStatus.NEEDS_CRM_ATTENTION);
                room.setNeedsAttention(true);
            }
            case INTERESTED -> {
                room.setStatus(ChatRoomStatus.ACTIVE);
                room.setNeedsAttention(false);
            }
        }
    }

    private AdminChatDecisionLogResponse toDecisionLogResponse(ChatFamilyDecisionLog log) {
        return AdminChatDecisionLogResponse.builder()
                .decisionId(log.getId())
                .roomId(log.getRoom().getId())
                .decision(log.getDecision())
                .note(log.getNote())
                .nextFollowUpAt(log.getNextFollowUpAt())
                .createdByEmployeeId(log.getCreatedByEmployee() != null ? log.getCreatedByEmployee().getId() : null)
                .createdByName(log.getCreatedByName())
                .createdAt(log.getCreatedAt())
                .build();
    }
}