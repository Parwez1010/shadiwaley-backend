package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.chat.domain.*;
import com.shadiwaley.server.chat.dto.admin.request.AdminSendChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.AdminChatInternalNoteRequest;
import com.shadiwaley.server.chat.dto.response.*;
import com.shadiwaley.server.chat.dto.websocket.AdminChatMonitorEvent;
import com.shadiwaley.server.chat.infrastructure.entity.*;
import com.shadiwaley.server.chat.infrastructure.repository.*;
import com.shadiwaley.server.communication.application.service.CommunicationCenterEventPublisher;
import com.shadiwaley.server.communication.domain.CommunicationCenterEventType;
import com.shadiwaley.server.communication.dto.websocket.CommunicationCenterEvent;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.media.application.service.MediaService;
import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import com.shadiwaley.server.revenue.infrastructure.repository.FamilySubscriptionRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AdminChatMonitorService {

    private final FamilyChatRoomRepository chatRoomRepository;
    private final FamilyChatMessageRepository chatMessageRepository;
    private final ChatRoomInternalNoteRepository noteRepository;
    private final EmployeeAccountRepository employeeAccountRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final AuditLogService auditLogService;
    private final MediaFileRepository mediaFileRepository;
    private final AdminChatMonitorEventPublisher adminChatMonitorEventPublisher;
    private final FileStorageService fileStorageService;
    private final ChatMessageReportRepository reportRepository;
    private final FamilySubscriptionRepository familySubscriptionRepository;
    private final MediaService mediaService;
    private final UserAccountRepository userAccountRepositoryl;
    private final CommunicationCenterEventPublisher communicationCenterEventPublisher;
    private final UserProfileRepository userProfileRepository;


    @Transactional(readOnly = true)
    public AdminChatRoomPageResponse getRooms(
            int page,
            int size,
            ChatRoomStatus status,
            ChatMode chatMode,
            Boolean reported,
            Boolean needsAttention,
            Boolean blocked,
            UUID assignedEmployeeId,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Specification<FamilyChatRoom> spec = buildRoomSpec(
                actor,
                status,
                chatMode,
                reported,
                needsAttention,
                blocked,
                assignedEmployeeId,
                search,
                fromDate,
                toDate
        );

        Page<FamilyChatRoom> result = chatRoomRepository.findAll(spec, pageable);

        return AdminChatRoomPageResponse.builder()
                .items(result.getContent().stream().map(this::toRoomResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }


    @Transactional(readOnly = true)
    public AdminChatRoomDetailResponse getRoomDetail(UUID roomId) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        return toRoomDetailResponse(room);
    }

    @Transactional(readOnly = true)
    public AdminChatMessagePageResponse getMessages(
            UUID roomId,
            Instant before,
            int limit
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<FamilyChatMessage> fetched = before == null
                ? chatMessageRepository.findByRoomIdAndDeletedAtIsNullOrderBySentAtDesc(
                roomId,
                PageRequest.of(0, safeLimit + 1)
        )
                : chatMessageRepository.findByRoomIdAndSentAtBeforeAndDeletedAtIsNullOrderBySentAtDesc(
                roomId,
                before,
                PageRequest.of(0, safeLimit + 1)
        );

        boolean hasMore = fetched.size() > safeLimit;

        List<FamilyChatMessage> page = hasMore
                ? fetched.subList(0, safeLimit)
                : fetched;

        List<AdminChatMessageResponse> messages = page.stream()
                .sorted(Comparator.comparing(FamilyChatMessage::getSentAt))
                .map(this::toMessageResponse)
                .toList();

        Instant nextCursor = page.isEmpty()
                ? null
                : page.get(page.size() - 1).getSentAt();

        return AdminChatMessagePageResponse.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Transactional
    public AdminChatInternalNoteResponse addInternalNote(
            UUID roomId,
            AdminChatInternalNoteRequest request
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        ChatRoomInternalNote note = new ChatRoomInternalNote();
        note.setRoom(room);
        note.setEmployee(actor);
        note.setNote(request.getNote());

        ChatRoomInternalNote saved = noteRepository.save(note);

        auditLogService.record(
                AuditAction.CHAT_INTERNAL_NOTE_ADDED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Internal chat note added"
        );

        return toNoteResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AdminChatInternalNoteResponse> getInternalNotes(UUID roomId) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        return noteRepository.findByRoomIdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(this::toNoteResponse)
                .toList();
    }

    private Specification<FamilyChatRoom> buildRoomSpec(
            EmployeeAccount actor,
            ChatRoomStatus status,
            ChatMode chatMode,
            Boolean reported,
            Boolean needsAttention,
            Boolean blocked,
            UUID assignedEmployeeId,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            /*
             * Role visibility:
             * SUPER_ADMIN / ADMIN -> all rooms
             * CRM_AGENT / SUPPORT_AGENT -> only assigned rooms
             */
            if (!isAdmin(actor)) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), actor.getId())
                );
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (chatMode != null) {
                predicates.add(cb.equal(root.get("chatMode"), chatMode));
            }

            if (reported != null) {
                predicates.add(cb.equal(root.get("reported"), reported));
            }

            if (needsAttention != null) {
                predicates.add(cb.equal(root.get("needsAttention"), needsAttention));
            }

            if (blocked != null) {
                predicates.add(cb.equal(root.get("blocked"), blocked));
            }

            if (assignedEmployeeId != null) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), assignedEmployeeId)
                );
            }

            if (fromDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                fromDate
                                        .atStartOfDay()
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                        )
                );
            }

            if (toDate != null) {
                predicates.add(
                        cb.lessThan(
                                root.get("createdAt"),
                                toDate
                                        .plusDays(1)
                                        .atStartOfDay()
                                        .atZone(ZoneId.systemDefault())
                                        .toInstant()
                        )
                );
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("lastMessageText")), pattern),
                                cb.like(cb.lower(root.get("lastMessageByName")), pattern),
                                cb.like(cb.lower(root.get("lastReportReason")), pattern),
                                cb.like(cb.lower(root.get("fromUser").get("phone")), pattern),
                                cb.like(cb.lower(root.get("toUser").get("phone")), pattern),
                                cb.like(cb.lower(root.get("fromProfile").get("candidateFirstName")), pattern),
                                cb.like(cb.lower(root.get("toProfile").get("candidateFirstName")), pattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminChatRoomResponse toRoomResponse(FamilyChatRoom room) {

        AdminChatParticipantResponse from = resolveFromParticipant(room);
        AdminChatParticipantResponse to = resolveToParticipant(room);

        UserAccount subscriptionOwner = room.getGirlUser();
        FamilySubscription subscription =
                getCurrentSubscription(subscriptionOwner.getId());

        boolean active =
                subscription != null
                        && subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE;


        boolean subscriptionActive = isActiveSubscription(subscription);

        boolean crmAllowed =
                isCrmAllowed(subscription);

        boolean crmAssigned =
                room.getAssignedEmployee() != null;

        return AdminChatRoomResponse.builder()
                .roomId(room.getId())
                .rishtaRequestId(room.getRishtaRequest() != null ? room.getRishtaRequest().getId() : null)
                .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getFullName() : null)
                .fromProfile(from)
                .toProfile(to)
                .crmAssistanceAllowed(crmAllowed)

                .crmAssigned(
                        room.getAssignedEmployee() != null
                )

                .canIntervene(crmAllowed)

                .canSendMessage(crmAllowed)
                .fromProfileId(from != null ? from.getProfileId() : null)
                .fromCandidateName(from != null ? from.getCandidateName() : null)
                .toProfileId(to != null ? to.getProfileId() : null)
                .toCandidateName(to != null ? to.getCandidateName() : null)
                .status(room.getStatus())
                .chatMode(room.getChatMode())
                .blocked(room.isBlocked())
                .reported(room.isReported())
                .planCode(subscription != null ? subscription.getPlanCode() : null)
                .planName(subscription != null ? subscription.getPlanName() : null)
                .subscriptionStatus(subscription != null ? subscription.getSubscriptionStatus().name() : null)
                .subscriptionActive(subscriptionActive)
                .subscriptionEndAt(subscription != null ? subscription.getEndAt() : null)
                .crmAssistanceAllowed(crmAllowed)
                .crmAssigned(crmAssigned)
                .canIntervene(crmAllowed)
                .canSendMessage(crmAllowed)
                .fromParentName(from != null ? from.getParentName() : null)
                .fromPhone(from != null ? from.getPhone() : null)
                .fromSide(from != null ? from.getSide() : null)
                .fromDistrict(from != null ? from.getDistrict() : null)

                .toParentName(to != null ? to.getParentName() : null)
                .toPhone(to != null ? to.getPhone() : null)
                .toSide(to != null ? to.getSide() : null)
                .toDistrict(to != null ? to.getDistrict() : null)

                .needsAttention(room.isNeedsAttention())
                .lastReportReason(room.getLastReportReason())
                .lastMessageText(room.getLastMessageText())
                .lastMessageType(room.getLastMessageType())
                .lastMessageAt(room.getLastMessageAt())
                .lastMessageByName(room.getLastMessageByName())
                .messageCount(room.getMessageCount())
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())

                .build();
    }

    private AdminChatRoomDetailResponse toRoomDetailResponse(FamilyChatRoom room) {

        AdminChatParticipantResponse from =
                resolveFromParticipant(room);

        AdminChatParticipantResponse to =
                resolveToParticipant(room);

        return AdminChatRoomDetailResponse.builder()
                .roomId(room.getId())
                .rishtaRequestId(room.getRishtaRequest() != null ? room.getRishtaRequest().getId() : null)
                .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getFullName() : null)
                .fromProfile(from)
                .toProfile(to)
                .status(room.getStatus())
                .statusLabel(resolveStatusLabel(room.getStatus()))
                .chatMode(room.getChatMode())
                .expectedSpeaker(resolveExpectedSpeaker(room))
                .blocked(room.isBlocked())
                .reported(room.isReported())
                .needsAttention(room.isNeedsAttention())
                .lastReportReason(room.getLastReportReason())
                .lastMessageText(room.getLastMessageText())
                .lastMessageAt(room.getLastMessageAt())
                .lastMessageByName(room.getLastMessageByName())
                .messageCount(room.getMessageCount())
                .unreadCount(0L)
                .attachmentCount(countAttachments(room.getId()))
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .closedAt(room.getClosedAt())
                .build();
    }


    private AdminChatParticipantResponse resolveFromParticipant(FamilyChatRoom room) {
        if (room.getFromUser() != null || room.getFromProfile() != null) {
            return toParticipant(room.getFromUser(), room.getFromProfile());
        }

        if (room.getRishtaRequest() != null) {
            return toParticipant(
                    room.getRishtaRequest().getSenderUser(),
                    room.getRishtaRequest().getSenderProfile()
            );
        }

        return toParticipant(room.getBoyUser(), null);
    }

    private AdminChatParticipantResponse resolveToParticipant(FamilyChatRoom room) {
        if (room.getToUser() != null || room.getToProfile() != null) {
            return toParticipant(room.getToUser(), room.getToProfile());
        }

        if (room.getRishtaRequest() != null) {
            return toParticipant(
                    room.getRishtaRequest().getReceiverUser(),
                    room.getRishtaRequest().getReceiverProfile()
            );
        }

        return toParticipant(room.getGirlUser(), null);
    }

    private String resolveStatusLabel(ChatRoomStatus status) {
        if (status == null) {
            return null;
        }

        return switch (status) {
            case ACTIVE -> "Active";
            case BLOCKED -> "Blocked";
            case CLOSED -> "Closed";
            case REPORTED -> "Reported";
            default -> status.name();
        };
    }



    private String resolveExpectedSpeaker(FamilyChatRoom room) {
        if (room.getChatMode() == ChatMode.DIRECT_FAMILY) {
            return "FAMILY_TO_FAMILY";
        }

        if (room.getChatMode() == ChatMode.CRM_ASSISTED) {
            return "CRM_ASSISTED";
        }

        if (room.getChatMode() == ChatMode.CRM_TO_CRM) {
            return "CRM_TO_CRM";
        }

        return "FAMILY_TO_FAMILY";
    }

    private Long countAttachments(UUID roomId) {
        return chatMessageRepository.countByRoomIdAndMediaFileIsNotNull(roomId);
    }


    private AdminChatParticipantResponse toParticipant(
            UserAccount user,
            UserProfile profile
    ) {
        if (user == null && profile == null) {
            return null;
        }

        UUID userId = user != null ? user.getId() : null;

        if (userId == null && profile != null && profile.getUserAccount() != null) {
            userId = profile.getUserAccount().getId();
            user = profile.getUserAccount();
        }

        ParentProfile parent = userId != null
                ? parentProfileRepository.findByUserAccountId(userId).orElse(null)
                : null;

        return AdminChatParticipantResponse.builder()
                .userId(userId)
                .familyUserId(userId)
                .profileId(profile != null ? profile.getId() : null)
                .displayId(profile != null ? profile.getDisplayId() : null)
                .candidateName(profile != null ? profile.getCandidateFirstName() : null)
                .parentName(parent != null ? parent.getParentName() : null)
                .parentPhone(parent != null ? parent.getParentPhone() : user != null ? user.getPhone() : null)
                .phone(user != null ? user.getPhone() : null)
                .side(user != null && user.getSide() != null ? user.getSide().name() : null)
                .age(profile != null ? profile.getCandidateAge() : null)
                .district(parent != null ? parent.getDistrict() : null)
                .state(parent != null ? parent.getState() : null)
                .maslak(parent != null ? parent.getMaslak() : null)
                .caste(parent != null ? parent.getCaste() : null)
                .education(profile != null ? profile.getEducation() : null)
                .professionTitle(profile != null ? profile.getProfessionTitle() : null)
                .profilePhotoViewUrl(profile != null
                        ? "/api/v1/admin/media/profiles/" + profile.getId() + "/primary-photo/view"
                        : null)
                .build();
    }


    private AdminChatMessageResponse toMessageResponse(FamilyChatMessage message) {
        FamilyChatMessage reply = message.getReplyToMessage();

        MediaFile media = message.getMediaFile();

        return AdminChatMessageResponse.builder()

                .messageId(message.getId())

                .roomId(message.getRoom().getId())

                .senderType(message.getSenderType())

                .senderUserId(
                        message.getSenderUser() != null
                                ? message.getSenderUser().getId()
                                : null
                )

                .senderEmployeeId(
                        message.getSenderEmployee() != null
                                ? message.getSenderEmployee().getId()
                                : null
                )

                .senderEmployeeName(
                        message.getSenderEmployee() != null
                                ? message.getSenderEmployee().getFullName()
                                : null
                )

                .senderDisplayName(resolveSenderName(message))
                .assistedUserId(
                        message.getAssistedUser() != null
                                ? message.getAssistedUser().getId()
                                : null
                )

                .assistedFamilyName(
                        message.getAssistedFamilyName()
                )

                .messageType(message.getMessageType())

                .content(message.getContent())

                .replyToMessageId(
                        message.getReplyToMessage() != null
                                ? message.getReplyToMessage().getId()
                                : null
                )

                .mediaFileId(
                        media != null
                                ? media.getId()
                                : null
                )

                .mediaType(
                        media != null
                                ? media.getMediaType()
                                : null
                )

                .mediaPreviewUrl(
                        media != null
                                ? "/api/v1/admin/chat-monitor/rooms/"
                                + message.getRoom().getId()
                                + "/messages/"
                                + message.getId()
                                + "/media/"
                                + media.getId()
                                + "/view"
                                : null
                )

                .fileName(
                        media != null
                                ? media.getOriginalFileName()
                                : null
                )

                .fileSizeBytes(
                        media != null
                                ? media.getFileSizeBytes()
                                : null
                )

                .contentType(
                        media != null
                                ? media.getContentType()
                                : null
                )

                .deliveryStatus(message.getDeliveryStatus())

                .moderationStatus(message.getModerationStatus())

                .editedAt(message.getEditedAt())

                .sentAt(message.getSentAt())

                .readAt(message.getReadAt())

                .build();
    }


    private String resolveSenderName(
            FamilyChatMessage message
    ) {

        if (message.getSenderEmployee() != null) {
            return message.getSenderEmployee().getFullName();
        }

        if (message.getSenderUser() != null) {

            ParentProfile parent =
                    parentProfileRepository
                            .findByUserAccountId(
                                    message.getSenderUser().getId()
                            )
                            .orElse(null);

            if (parent != null && parent.getParentName() != null) {
                return parent.getParentName();
            }

            UserProfile profile =
                    userProfileRepository
                            .findByUserAccountId(
                                    message.getSenderUser().getId()
                            )
                            .orElse(null);

            if (profile != null) {
                return profile.getCandidateFirstName();
            }

            return message.getSenderUser().getPhone();
        }

        return "Unknown";
    }


    private AdminChatInternalNoteResponse toNoteResponse(ChatRoomInternalNote note) {
        return AdminChatInternalNoteResponse.builder()
                .noteId(note.getId())
                .roomId(note.getRoom().getId())
                .employeeId(note.getEmployee().getId())
                .employeeName(note.getEmployee().getFullName())
                .note(note.getNote())
                .createdAt(note.getCreatedAt())
                .build();
    }


    private String preview(String content) {
        if (content == null) {
            return null;
        }

        return content.length() <= 80 ? content : content.substring(0, 80) + "...";
    }

    private EmployeeAccount getCurrentEmployee() {
        UUID employeeId = AuthUser.getCurrentActorId();

        return employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }

    private boolean isAdmin(EmployeeAccount employee) {
        return employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN;
    }

    private void assertCanAccessRoom(EmployeeAccount employee, FamilyChatRoom room) {
        if (isAdmin(employee)) {
            return;
        }

        if (room.getAssignedEmployee() == null
                || !room.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can access only assigned chat rooms");
        }
    }

    @Transactional(readOnly = true)
    public AdminChatDashboardResponse getDashboard() {

        return AdminChatDashboardResponse.builder()

                .totalRooms(chatRoomRepository.count())

                .activeRooms(
                        chatRoomRepository.countByStatus(ChatRoomStatus.ACTIVE)
                )

                .blockedRooms(
                        chatRoomRepository.countByBlockedTrue()
                )

                .closedRooms(
                        chatRoomRepository.countByStatus(ChatRoomStatus.CLOSED)
                )

                .reportedRooms(
                        chatRoomRepository.countByReportedTrue()
                )

                .needsAttentionRooms(
                        chatRoomRepository.countByNeedsAttentionTrue()
                )

                .unassignedRooms(
                        chatRoomRepository.countByAssignedEmployeeIsNull()
                )

                .crmAssignedRooms(
                        chatRoomRepository.countByAssignedEmployeeIsNotNull()
                )

                .totalMessages(
                        chatMessageRepository.count()
                )

                .build();
    }

    @Transactional
    public void assignRoom(
            UUID roomId,
            UUID employeeId
    ) {

        EmployeeAccount actor = getCurrentEmployee();

        if (!isAdmin(actor)) {
            throw new AccessDeniedException(
                    "Only admin can assign rooms"
            );
        }

        FamilyChatRoom room =
                chatRoomRepository.findById(roomId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Room not found")
                        );

        EmployeeAccount employee =
                employeeAccountRepository.findById(employeeId)
                        .orElseThrow(
                                () -> new EntityNotFoundException("Employee not found")
                        );

        room.setAssignedEmployee(employee);

        chatRoomRepository.save(room);

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_ROOM_ASSIGNED)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Chat room assigned")
                        .message("A chat room was assigned to a CRM/admin.")
                        .emittedAt(Instant.now())
                        .build()
        );

        auditLogService.record(
                AuditAction.CHAT_ASSIGNMENT_UPDATED,
                AuditEntityType.CHAT_ROOM,
                roomId,
                "Chat room assigned"
        );
    }

    @Transactional
    public void blockRoom(UUID roomId){

        FamilyChatRoom room=findRoom(roomId);

        room.setBlocked(true);

        room.setStatus(ChatRoomStatus.BLOCKED);

        chatRoomRepository.save(room);

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_ROOM_BLOCKED)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Chat room blocked")
                        .message("A chat room has been blocked.")
                        .emittedAt(Instant.now())
                        .build()
        );

        auditLogService.record(
                AuditAction.CHAT_ROOM_BLOCKED,
                AuditEntityType.CHAT_ROOM,
                roomId,
                "Chat room blocked"
        );
    }

    @Transactional
    public void unblockRoom(UUID roomId){

        FamilyChatRoom room=findRoom(roomId);

        room.setBlocked(false);

        room.setStatus(ChatRoomStatus.ACTIVE);

        chatRoomRepository.save(room);

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_ROOM_UNBLOCKED)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Chat room unblocked")
                        .message("A chat room has been unblocked.")
                        .emittedAt(Instant.now())
                        .build()
        );


        auditLogService.record(
                AuditAction.CHAT_ROOM_UNBLOCKED,
                AuditEntityType.CHAT_ROOM,
                roomId,
                "Chat room unblocked"
        );
    }

    @Transactional
    public void closeRoomAdmin(UUID roomId){

        FamilyChatRoom room=findRoom(roomId);

        room.setStatus(ChatRoomStatus.CLOSED);

        room.setClosedAt(Instant.now());

        chatRoomRepository.save(room);

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_ROOM_CLOSED)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Chat room closed")
                        .message("A chat room has been closed.")
                        .emittedAt(Instant.now())
                        .build()
        );


        auditLogService.record(
                AuditAction.CHAT_ROOM_CLOSED,
                AuditEntityType.CHAT_ROOM,
                roomId,
                "Closed by admin"
        );
    }

    @Transactional
    public void hideMessage(UUID messageId){

        FamilyChatMessage message=findMessage(messageId);

        message.setModerationStatus(
                ChatModerationStatus.HIDDEN
        );

        chatMessageRepository.save(message);

        FamilyChatRoom room = message.getRoom();

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_MESSAGE_HIDDEN)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Message hidden")
                        .message("A chat message was hidden by admin.")
                        .emittedAt(Instant.now())
                        .build()
        );

        auditLogService.record(
                AuditAction.CHAT_MESSAGE_MODERATED,
                AuditEntityType.CHAT_MESSAGE,
                messageId,
                "Message hidden"
        );
    }

    @Transactional
    public void restoreMessage(UUID messageId){

        FamilyChatMessage message=findMessage(messageId);

        message.setModerationStatus(
                ChatModerationStatus.CLEAN
        );

        chatMessageRepository.save(message);

        FamilyChatRoom room = message.getRoom();

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_MESSAGE_RESTORED)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Message restored")
                        .message("A chat message was restored by admin.")
                        .emittedAt(Instant.now())
                        .build()
        );


        auditLogService.record(
                AuditAction.CHAT_MESSAGE_MODERATED,
                AuditEntityType.CHAT_MESSAGE,
                messageId,
                "Message restored"
        );
    }

    private FamilyChatRoom findRoom(UUID id){

        return chatRoomRepository.findById(id)
                .orElseThrow(
                        ()->new EntityNotFoundException("Room not found")
                );
    }

    private FamilyChatMessage findMessage(UUID id){

        return chatMessageRepository.findById(id)
                .orElseThrow(
                        ()->new EntityNotFoundException("Message not found")
                );
    }


    @Transactional
    public AdminChatMessageResponse sendCrmMessage(
            UUID roomId,
            AdminSendChatMessageRequest request
    ) {
        validateCrmMessageRequest(request);

        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        if (room.isBlocked()) {
            throw new IllegalArgumentException("Blocked room cannot receive messages");
        }

        if (room.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active rooms can receive messages");
        }

        UserAccount assistedUser = resolveAssistedUser(room, request.getAssistedUserId());

        FamilyChatMessage replyTo = null;

        if (request.getReplyToMessageId() != null) {
            replyTo = chatMessageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new EntityNotFoundException("Reply message not found"));

            if (!replyTo.getRoom().getId().equals(room.getId())) {
                throw new IllegalArgumentException("Reply message does not belong to this room");
            }
        }

        MediaFile media = null;

        if (isMediaMessage(request.getMessageType())) {
            media = mediaFileRepository.findByIdAndDeletedFalse(request.getMediaFileId())
                    .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

            if (!media.getUserAccount().getId().equals(assistedUser.getId())) {
                throw new IllegalArgumentException("Media does not belong to assisted family");
            }
        }

        String content = request.getContent() == null ? "" : request.getContent().trim();

        FamilyChatMessage message = new FamilyChatMessage();
        message.setRoom(room);
        message.setSenderUser(assistedUser);
        message.setSenderType(ChatSenderType.CRM_AGENT);
        message.setSenderEmployee(actor);
        message.setAssistedUser(assistedUser);
        message.setAssistedFamilyName(resolveFamilyName(assistedUser));
        message.setMessageType(request.getMessageType());
        message.setContent(content);
        message.setMediaFile(media);
        message.setReplyToMessage(replyTo);
        message.setDeliveryStatus(ChatDeliveryStatus.SENT);
        message.setDeliveredAt(Instant.now());

        FamilyChatMessage saved = chatMessageRepository.save(message);

        room.setLastMessageType(saved.getMessageType());
        room.setLastMessageText(
                isMediaMessage(saved.getMessageType()) && media != null
                        ? media.getOriginalFileName()
                        : preview(content)
        );
        room.setLastMessageAt(saved.getSentAt());
        room.setLastMessageByName(actor.getFullName());
        room.setMessageCount(room.getMessageCount() + 1);
        room.setUpdatedAt(Instant.now());

        chatRoomRepository.save(room);

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_CRM_MESSAGE_SENT)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title(media != null ? "CRM media message sent" : "CRM message sent")
                        .message(media != null
                                ? "A CRM media message was sent in a family chat."
                                : "A CRM message was sent in a family chat.")
                        .emittedAt(Instant.now())
                        .build()
        );

        publishCommunicationCenterEventForCrmMessage(room, saved);

        auditLogService.record(
                AuditAction.CHAT_MESSAGE_SENT_BY_CRM,
                AuditEntityType.CHAT_MESSAGE,
                saved.getId(),
                media != null
                        ? "CRM sent media message in family chat"
                        : "CRM sent message in family chat"
        );

        return toMessageResponse(saved);
    }

    private boolean isMediaMessage(ChatMessageType messageType) {
        return messageType == ChatMessageType.IMAGE
                || messageType == ChatMessageType.DOCUMENT;
    }


    private void validateCrmMessageRequest(AdminSendChatMessageRequest request) {
        if (request.getAssistedUserId() == null) {
            throw new IllegalArgumentException("Assisted user is required");
        }

        if (request.getMessageType() == null) {
            throw new IllegalArgumentException("Message type is required");
        }

        if (request.getMessageType() == ChatMessageType.TEXT
                && (request.getContent() == null || request.getContent().isBlank())) {
            throw new IllegalArgumentException("Message content is required");
        }

        if (isMediaMessage(request.getMessageType())
                && request.getMediaFileId() == null) {
            throw new IllegalArgumentException("Media file is required");
        }

        if (request.getMessageType() == ChatMessageType.SYSTEM) {
            throw new IllegalArgumentException("CRM cannot send SYSTEM messages");
        }

        if (request.getContent() != null && request.getContent().length() > 1000) {
            throw new IllegalArgumentException("Message content must be less than 1000 characters");
        }
    }



    private UserAccount resolveAssistedUser(
            FamilyChatRoom room,
            UUID assistedUserId
    ) {
        if (room.getBoyUser() != null && room.getBoyUser().getId().equals(assistedUserId)) {
            return room.getBoyUser();
        }

        if (room.getGirlUser() != null && room.getGirlUser().getId().equals(assistedUserId)) {
            return room.getGirlUser();
        }

        throw new AccessDeniedException("Assisted user must be a participant of this room");
    }

    private String resolveFamilyName(UserAccount user) {
        if (user == null) {
            return null;
        }

        return parentProfileRepository.findByUserAccountId(user.getId())
                .map(ParentProfile::getParentName)
                .orElse(user.getPhone());
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> viewMessageMedia(
            UUID roomId,
            UUID messageId,
            UUID mediaId
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        FamilyChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getRoom().getId().equals(room.getId())) {
            throw new AccessDeniedException("Message does not belong to this chat room");
        }

        if (message.getMediaFile() == null
                || !message.getMediaFile().getId().equals(mediaId)) {
            throw new EntityNotFoundException("Media not found for this message");
        }

        MediaFile media = message.getMediaFile();

        if (media.isDeleted()) {
            throw new EntityNotFoundException("Media file not found");
        }

        byte[] bytes = fileStorageService.load(media.getStorageKey());

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.CHAT_MESSAGE,
                message.getId(),
                "Admin viewed chat media"
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + media.getOriginalFileName() + "\""
                )
                .body(bytes);
    }

    @Transactional(readOnly = true)
    public AdminChatReportPageResponse getReports(int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ChatMessageReport> result =
                reportRepository.findAllByOrderByCreatedAtDesc(pageable);

        return AdminChatReportPageResponse.builder()
                .items(result.getContent().stream().map(this::toReportResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @Transactional
    public void resolveReport(UUID reportId) {
        ChatMessageReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found"));

        FamilyChatMessage message = report.getMessage();

        message.setModerationStatus(ChatModerationStatus.CLEAN);
        chatMessageRepository.save(message);

        FamilyChatRoom room = message.getRoom();

        room.setNeedsAttention(false);
        room.setReported(false);
        room.setLastReportReason(null);

        if (room.getStatus() == ChatRoomStatus.REPORTED) {
            room.setStatus(ChatRoomStatus.ACTIVE);
        }

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_MESSAGE_MODERATED,
                AuditEntityType.CHAT_MESSAGE,
                message.getId(),
                "Chat report resolved"
        );
    }

    private AdminChatReportResponse toReportResponse(ChatMessageReport report) {
        FamilyChatMessage message = report.getMessage();

        return AdminChatReportResponse.builder()
                .reportId(report.getId())
                .roomId(message.getRoom().getId())
                .messageId(message.getId())
                .reporterUserId(report.getReporterUser().getId())
                .reporterName(resolveReporterName(report.getReporterUser()))
                .reporterPhone(report.getReporterUser().getPhone())
                .reason(report.getReason())
                .details(report.getDetails())
                .messageContent(message.getContent())
                .messageSenderName(resolveSenderName(message))
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String resolveReporterName(UserAccount user) {
        return parentProfileRepository.findByUserAccountId(user.getId())
                .map(ParentProfile::getParentName)
                .orElse(user.getPhone());
    }



    private FamilySubscription getCurrentSubscription(UUID userId) {
        return familySubscriptionRepository
                .findFirstByUserAccountIdAndCurrentSubscriptionTrue(userId)
                .orElse(null);
    }

    private boolean isActiveSubscription(FamilySubscription subscription) {
        return subscription != null
                && subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE;
    }

    private boolean isCrmAllowed(FamilySubscription subscription) {
        if (!isActiveSubscription(subscription)) {
            return false;
        }

        return switch (subscription.getPlanCode()) {
            case "PREMIUM", "ELITE" -> true;
            default -> false;
        };
    }

    private FamilySubscription resolveBestSubscription(FamilyChatRoom room) {
        FamilySubscription boySubscription =
                room.getBoyUser() != null
                        ? getCurrentSubscription(room.getBoyUser().getId())
                        : null;

        FamilySubscription girlSubscription =
                room.getGirlUser() != null
                        ? getCurrentSubscription(room.getGirlUser().getId())
                        : null;

        if (isCrmAllowed(boySubscription)) {
            return boySubscription;
        }

        if (isCrmAllowed(girlSubscription)) {
            return girlSubscription;
        }

        if (isActiveSubscription(boySubscription)) {
            return boySubscription;
        }

        if (isActiveSubscription(girlSubscription)) {
            return girlSubscription;
        }

        return boySubscription != null ? boySubscription : girlSubscription;
    }

    @Transactional
    public AdminChatMediaUploadResponse uploadMedia(
            UUID roomId,
            UUID assistedUserId,
            MultipartFile file
    ) {

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Chat room not found"));

        validateEmployeeRoomAccess(room);

        if (room.getStatus() == ChatRoomStatus.CLOSED) {
            throw new IllegalArgumentException("Chat room is closed");
        }

        if (room.isBlocked()) {
            throw new IllegalArgumentException("Chat room is blocked");
        }

        UUID fromUser =
                room.getBoyUser() != null
                        ? room.getBoyUser().getId()
                        : null;

        UUID toUser =
                room.getGirlUser() != null
                        ? room.getGirlUser().getId()
                        : null;

        if (!assistedUserId.equals(fromUser)
                && !assistedUserId.equals(toUser)) {
            throw new IllegalArgumentException(
                    "Assisted user does not belong to this chat room"
            );
        }

        return mediaService.uploadCrmChatMedia(
                roomId,
                assistedUserId,
                file
        );
    }

    private void validateEmployeeRoomAccess(
            FamilyChatRoom room
    ) {

        EmployeeAccount employee = getCurrentEmployee();

        if (employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN) {
            return;
        }

        if (employee.getRole() == EmployeeRole.CRM_AGENT) {

            if (room.getAssignedEmployee() == null
                    || !room.getAssignedEmployee()
                    .getId()
                    .equals(employee.getId())) {

                throw new IllegalArgumentException(
                        "You are not assigned to this chat room"
                );
            }

            return;
        }

        throw new IllegalArgumentException(
                "You do not have permission"
        );
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> downloadMessageMedia(
            UUID roomId,
            UUID messageId,
            UUID mediaId
    ) {
        return streamMessageMedia(
                roomId,
                messageId,
                mediaId,
                true
        );
    }


    private ResponseEntity<byte[]> streamMessageMedia(
            UUID roomId,
            UUID messageId,
            UUID mediaId,
            boolean download
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        assertCanAccessRoom(actor, room);

        FamilyChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getRoom().getId().equals(room.getId())) {
            throw new AccessDeniedException("Message does not belong to this chat room");
        }

        if (message.getMediaFile() == null
                || !message.getMediaFile().getId().equals(mediaId)) {
            throw new EntityNotFoundException("Media not found for this message");
        }

        MediaFile media = message.getMediaFile();

        if (media.isDeleted()) {
            throw new EntityNotFoundException("Media file not found");
        }

        byte[] bytes = fileStorageService.load(media.getStorageKey());

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.CHAT_MESSAGE,
                message.getId(),
                download
                        ? "Admin downloaded chat media"
                        : "Admin viewed chat media"
        );

        String disposition = download ? "attachment" : "inline";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + media.getOriginalFileName() + "\""
                )
                .body(bytes);
    }

    private void publishCommunicationCenterEventForCrmMessage(
            FamilyChatRoom room,
            FamilyChatMessage message
    ) {
        communicationCenterEventPublisher.publish(
                CommunicationCenterEvent.builder()
                        .event(CommunicationCenterEventType.CHAT_CRM_MESSAGE_SENT)
                        .itemType("CHAT_ROOM")
                        .itemId(room.getId())
                        .customerUserId(
                                room.getFromUser() != null
                                        ? room.getFromUser().getId()
                                        : null
                        )
                        .assignedEmployeeId(
                                room.getAssignedEmployee() != null
                                        ? room.getAssignedEmployee().getId()
                                        : null
                        )
                        .title(
                                message.getMediaFile() != null
                                        ? "CRM media message sent"
                                        : "CRM message sent"
                        )
                        .message(
                                message.getMediaFile() != null
                                        ? "A CRM media message was sent in family chat."
                                        : "A CRM message was sent in family chat."
                        )
                        .emittedAt(Instant.now())
                        .build()
        );
    }


}