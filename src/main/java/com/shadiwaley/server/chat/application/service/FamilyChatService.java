package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.chat.domain.*;
import com.shadiwaley.server.chat.dto.request.ReportChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.SendChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.UpdateChatMessageRequest;
import com.shadiwaley.server.chat.dto.response.*;
import com.shadiwaley.server.chat.dto.websocket.AdminChatMonitorEvent;
import com.shadiwaley.server.chat.dto.websocket.ChatWebSocketEvent;
import com.shadiwaley.server.chat.infrastructure.entity.*;
import com.shadiwaley.server.chat.infrastructure.repository.*;
import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.proposal.infrastructure.entity.Proposal;
import com.shadiwaley.server.proposal.infrastructure.repository.ProposalRepository;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.safety.application.service.UserSafetyService;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.subscription.domain.SubscriptionStatus;
import com.shadiwaley.server.subscription.infrastructure.repository.SubscriptionRepository;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FamilyChatService {

    private final FamilyChatRoomRepository chatRoomRepository;
    private final FamilyChatMessageRepository chatMessageRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MediaFileRepository mediaFileRepository;
    private final ChatMessageReportRepository reportRepository;
    private final ChatPresenceService chatPresenceService;
    private final AuditLogService auditLogService;
    private final UserSafetyService userSafetyService;
    private final ProposalRepository proposalRepository;

    private final SubscriptionRepository subscriptionRepository;
    private final AdminChatMonitorEventPublisher adminChatMonitorEventPublisher;
    private final FileStorageService fileStorageService;


    @Transactional
    public FamilyChatRoom createRoomForAcceptedRishta(RishtaRequest rishtaRequest) {
        if (rishtaRequest.getStatus() != RishtaRequestStatus.ACCEPTED) {
            throw new IllegalArgumentException("Chat room can be created only for accepted rishta requests");
        }

        return chatRoomRepository.findByRishtaRequestId(rishtaRequest.getId())
                .orElseGet(() -> {
                    FamilyChatRoom room = new FamilyChatRoom();
                    room.setRishtaRequest(rishtaRequest);

                    room.setFromUser(rishtaRequest.getSenderUser());
                    room.setToUser(rishtaRequest.getReceiverUser());
                    room.setFromProfile(rishtaRequest.getSenderProfile());
                    room.setToProfile(rishtaRequest.getReceiverProfile());

                    if (rishtaRequest.getSenderUser().getSide() == UserSide.BOY) {
                        room.setBoyUser(rishtaRequest.getSenderUser());
                        room.setGirlUser(rishtaRequest.getReceiverUser());
                    } else {
                        room.setBoyUser(rishtaRequest.getReceiverUser());
                        room.setGirlUser(rishtaRequest.getSenderUser());
                    }
                    room.setChatMode(
                            resolveChatMode(
                                    room.getBoyUser().getId(),
                                    room.getGirlUser().getId()
                            )
                    );

                    /*
                     * Admin Chat Monitor linkage:
                     * Proposal -> CRM Case -> Assigned CRM Employee
                     */
                    linkProposalAndCrm(room, rishtaRequest);

                    return chatRoomRepository.save(room);
                });
    }

    private ChatMode resolveChatMode(
            UUID boyUserId,
            UUID girlUserId
    ) {
        boolean boyHasCrmSupport = hasCrmSupportPlan(boyUserId);
        boolean girlHasCrmSupport = hasCrmSupportPlan(girlUserId);

        if (boyHasCrmSupport && girlHasCrmSupport) {
            return ChatMode.CRM_TO_CRM;
        }

        if (boyHasCrmSupport || girlHasCrmSupport) {
            return ChatMode.CRM_ASSISTED;
        }

        return ChatMode.DIRECT_FAMILY;
    }


    private boolean hasPaidPlan(UUID userId) {

        return subscriptionRepository
                .findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        SubscriptionStatus.ACTIVE
                )
                .filter(subscription ->
                        subscription.getExpiresAt() == null
                                || subscription.getExpiresAt().isAfter(Instant.now())
                )
                .map(subscription -> isPaidPlan(subscription.getPlanCodeSnapshot()))
                .orElse(false);
    }

    private boolean hasCrmSupportPlan(UUID userId) {

        return subscriptionRepository
                .findTopByUserAccountIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        SubscriptionStatus.ACTIVE
                )
                .filter(subscription ->
                        subscription.getExpiresAt() == null
                                || subscription.getExpiresAt().isAfter(Instant.now())
                )
                .map(subscription -> isPaidPlan(subscription.getPlanCodeSnapshot()))
                .orElse(false);
    }

    private boolean isPaidPlan(String planCode) {

        if (planCode == null || planCode.isBlank()) {
            return false;
        }

        return switch (planCode) {
            case "BASIC_299",
                 "PREMIUM_999",
                 "ELITE_2499" -> true;

            case "FREE_ONBOARDING" -> false;

            default -> false;
        };
    }



    private void linkProposalAndCrm(
            FamilyChatRoom room,
            RishtaRequest rishtaRequest
    ) {
        if (rishtaRequest.getSenderProfile() == null
                || rishtaRequest.getReceiverProfile() == null) {
            return;
        }

        UUID senderProfileId = rishtaRequest.getSenderProfile().getId();
        UUID receiverProfileId = rishtaRequest.getReceiverProfile().getId();

        Proposal proposal = proposalRepository
                .findTopByFromProfileIdAndToProfileIdOrderByCreatedAtDesc(
                        senderProfileId,
                        receiverProfileId
                )
                .or(() -> proposalRepository
                        .findTopByToProfileIdAndFromProfileIdOrderByCreatedAtDesc(
                                receiverProfileId,
                                senderProfileId
                        ))
                .orElse(null);

        if (proposal == null) {
            return;
        }

        room.setProposal(proposal);
        room.setCrmCase(proposal.getCrmCase());

        if (proposal.getCrmCase() != null) {
            room.setAssignedEmployee(proposal.getCrmCase().getAssignedEmployee());
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyRooms() {
        UUID userId = AuthUser.getCurrentUserId();

        return chatRoomRepository.findByBoyUserIdOrGirlUserIdOrderByUpdatedAtDesc(userId, userId)
                .stream()
                .filter(room -> room.getStatus() == ChatRoomStatus.ACTIVE)
                .map(room -> toRoomResponse(room, userId))
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(UUID roomId, Instant before, int limit) {
        UUID userId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, userId);
        int safeLimit = Math.min(Math.max(limit, 1), 100);

        List<FamilyChatMessage> fetched = before == null
                ? chatMessageRepository.findByRoomIdAndDeletedAtIsNullOrderBySentAtDesc(
                room.getId(),
                PageRequest.of(0, safeLimit + 1)
        )
                : chatMessageRepository.findByRoomIdAndSentAtBeforeAndDeletedAtIsNullOrderBySentAtDesc(
                room.getId(),
                before,
                PageRequest.of(0, safeLimit + 1)
        );


        boolean hasMore = fetched.size() > safeLimit;

        List<FamilyChatMessage> page = hasMore
                ? fetched.subList(0, safeLimit)
                : fetched;

        List<ChatMessageResponse> messages = page.stream()
                .filter(this::visibleToCustomer)
                .sorted(Comparator.comparing(FamilyChatMessage::getSentAt))
                .map(message -> toMessageResponse(message, userId))
                .toList();

        Instant nextCursor = page.isEmpty() ? null : page.get(page.size() - 1).getSentAt();

        return ChatMessagePageResponse.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID roomId, SendChatMessageRequest request) {
        UUID senderUserId = AuthUser.getCurrentUserId();
        return sendMessageInternal(roomId, senderUserId, request);
    }

    @Transactional
    public ChatMessageResponse sendMessageFromWebSocket(UUID roomId, UUID senderUserId, SendChatMessageRequest request) {
        return sendMessageInternal(roomId, senderUserId, request);
    }

    private ChatMessageResponse sendMessageInternal(UUID roomId, UUID senderUserId, SendChatMessageRequest request) {

        validateRateLimit(senderUserId);
        chatPresenceService.markOnline(senderUserId);

        FamilyChatRoom room = getAuthorizedRoom(roomId, senderUserId);

        if (room.isBlocked()) {
            throw new IllegalArgumentException("This chat room is blocked");
        }

        if (room.getStatus() != ChatRoomStatus.ACTIVE) {
            throw new IllegalArgumentException("This chat room is not active");
        }

        UserAccount sender = userAccountRepository.findById(senderUserId)
                .orElseThrow(() -> new EntityNotFoundException("Sender account not found"));

        FamilyChatMessage replyTo = null;

        if (request.getReplyToMessageId() != null) {
            replyTo = chatMessageRepository.findById(request.getReplyToMessageId())
                    .orElseThrow(() -> new EntityNotFoundException("Reply message not found"));

            if (!replyTo.getRoom().getId().equals(roomId)) {
                throw new IllegalArgumentException("Reply message does not belong to this chat room");
            }
        }

        MediaFile media = null;

        if (request.getMediaFileId() != null) {

            media = mediaFileRepository.findById(request.getMediaFileId())
                    .orElseThrow(() -> new EntityNotFoundException("Media file not found"));

            boolean owner = media.getUserAccount().getId().equals(senderUserId);

            if (!owner) {
                throw new IllegalArgumentException("You can only send your own uploaded media");
            }
        }

        FamilyChatMessage message = new FamilyChatMessage();
        message.setRoom(room);
        message.setSenderUser(sender);

// New sender tracking fields
        message.setSenderType(ChatSenderType.CUSTOMER);
        message.setSenderEmployee(null);
        message.setAssistedUser(null);
        message.setAssistedFamilyName(null);

        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setMediaFile(media);
        message.setReplyToMessage(replyTo);
        message.setDeliveryStatus(ChatDeliveryStatus.SENT);
        message.setDeliveredAt(Instant.now());

        FamilyChatMessage saved = chatMessageRepository.save(message);

        updateRoomReadModelAfterMessage(room, saved, sender);
        chatRoomRepository.save(room);

        UUID receiverUserId = getOtherUserId(room, senderUserId);

        if (userSafetyService.isBlockedBetween(senderUserId, receiverUserId)) {
            throw new IllegalArgumentException("You cannot send messages in this chat");
        }
        notificationService.create(
                receiverUserId,
                NotificationType.CHAT_OPENED,
                "New family message",
                "You have received a new message from a connected family.",
                "/chat/" + room.getId(),
                room.getId()
        );

        ChatMessageResponse response = toMessageResponse(saved, senderUserId);

        publishRoomEvent(
                room.getId(),
                ChatSocketEventType.MESSAGE_RECEIVED,
                response
        );

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_NEW_CHAT_MESSAGE)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("New family message")
                        .message("A new message was sent in a monitored family chat.")
                        .emittedAt(Instant.now())
                        .build()
        );


        return response;
    }

    @Transactional
    public ChatMessageResponse editMessage(UUID roomId, UUID messageId, UpdateChatMessageRequest request) {
        UUID userId = AuthUser.getCurrentUserId();

        getAuthorizedRoom(roomId, userId);

        FamilyChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Message does not belong to this room");
        }

        if (!message.getSenderUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can edit only your own messages");
        }

        if (message.getDeletedAt() != null) {
            throw new IllegalArgumentException("Deleted message cannot be edited");
        }

        message.setContent(request.getContent());
        message.setEditedAt(Instant.now());

        FamilyChatMessage saved = chatMessageRepository.save(message);
        ChatMessageResponse response = toMessageResponse(saved, userId);

        publishRoomEvent(
                roomId,
                ChatSocketEventType.MESSAGE_EDITED,
                response
        );
        return response;
    }

    @Transactional
    public void deleteMessage(UUID roomId, UUID messageId) {
        UUID userId = AuthUser.getCurrentUserId();

        getAuthorizedRoom(roomId, userId);

        FamilyChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Message does not belong to this room");
        }

        if (!message.getSenderUser().getId().equals(userId)) {
            throw new IllegalArgumentException("You can delete only your own messages");
        }

        if (message.getDeletedAt() != null) {
            return;
        }

        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        message.setDeletedAt(Instant.now());
        message.setDeletedByUser(user);
        message.setContent("This message was deleted.");

        FamilyChatMessage saved = chatMessageRepository.save(message);

        publishRoomEvent(
                roomId,
                ChatSocketEventType.MESSAGE_DELETED,
                toMessageResponse(saved, userId)
        );
    }

    @Transactional
    public void markRoomAsRead(UUID roomId) {
        UUID userId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, userId);

        List<FamilyChatMessage> unread = chatMessageRepository
                .findByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(room.getId(), userId);

        Instant now = Instant.now();

        unread.forEach(message -> {
            message.setReadAt(now);
            message.setDeliveryStatus(ChatDeliveryStatus.READ);
            chatMessageRepository.save(message);
        });

        publishRoomEvent(
                roomId,
                ChatSocketEventType.READ_RECEIPT,
                Map.of(
                        "roomId", roomId,
                        "readerUserId", userId,
                        "readAt", now
                )
        );
    }

    private FamilyChatRoom getAuthorizedRoom(UUID roomId, UUID userId) {
        FamilyChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found"));

        boolean participant = room.getBoyUser().getId().equals(userId)
                || room.getGirlUser().getId().equals(userId);

        if (!participant) {
            throw new IllegalArgumentException("You are not allowed to access this chat room");
        }

        return room;
    }

    private ChatRoomResponse toRoomResponse(FamilyChatRoom room, UUID currentUserId) {
        UUID otherUserId = getOtherUserId(room, currentUserId);

        UserProfile otherProfile = userProfileRepository.findByUserAccountId(otherUserId)
                .orElseThrow(() -> new EntityNotFoundException("Other user profile not found"));

        FamilyChatMessage lastMessage = chatMessageRepository
                .findByRoomIdAndDeletedAtIsNullOrderBySentAtDesc(
                        room.getId(),
                        PageRequest.of(0, 1)
                )
                .stream()
                .findFirst()
                .orElse(null);

        long unreadCount = chatMessageRepository
                .countByRoomIdAndSenderUserIdNotAndReadAtIsNullAndDeletedAtIsNull(room.getId(), currentUserId);

        boolean boyHasCrmSupport =
                hasCrmSupportPlan(room.getBoyUser().getId());

        boolean girlHasCrmSupport =
                hasCrmSupportPlan(room.getGirlUser().getId());

        String expectedSpeaker =
                resolveExpectedSpeaker(room, currentUserId, boyHasCrmSupport, girlHasCrmSupport);

        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .rishtaRequestId(room.getRishtaRequest().getId())
                .otherUserId(otherUserId)
                .otherProfileId(otherProfile.getId())
                .otherDisplayId(otherProfile.getDisplayId())
                .otherName(otherProfile.getCandidateFirstName())
                .status(room.getStatus())
                .chatMode(room.getChatMode() != null ? room.getChatMode().name() : null)
                .boyHasCrmSupport(boyHasCrmSupport)
                .girlHasCrmSupport(girlHasCrmSupport)
                .expectedSpeaker(expectedSpeaker)
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : null)
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }

    private String resolveExpectedSpeaker(
            FamilyChatRoom room,
            UUID currentUserId,
            boolean boyHasCrmSupport,
            boolean girlHasCrmSupport
    ) {
        if (room.getChatMode() == ChatMode.DIRECT_FAMILY) {
            return "FAMILY";
        }

        if (room.getChatMode() == ChatMode.CRM_TO_CRM) {
            return "CRM";
        }

        boolean currentUserIsBoy =
                room.getBoyUser().getId().equals(currentUserId);

        boolean currentUserHasCrmSupport =
                currentUserIsBoy ? boyHasCrmSupport : girlHasCrmSupport;

        if (currentUserHasCrmSupport) {
            return "CRM_ASSISTS_YOU";
        }

        return "FAMILY";
    }

    private ChatMessageResponse toMessageResponse(FamilyChatMessage message, UUID currentUserId) {
        UserProfile senderProfile = userProfileRepository.findByUserAccountId(message.getSenderUser().getId())
                .orElse(null);

        FamilyChatMessage reply = message.getReplyToMessage();

        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())

                .senderUserId(message.getSenderUser() != null ? message.getSenderUser().getId() : null)
                .senderDisplayName(senderProfile != null ? senderProfile.getCandidateFirstName() : "Family")

                .senderType(message.getSenderType())

                .senderEmployeeId(message.getSenderEmployee() != null
                        ? message.getSenderEmployee().getId()
                        : null)

                .senderEmployeeName(message.getSenderEmployee() != null
                        ? message.getSenderEmployee().getFullName()
                        : null)

                .assistedUserId(message.getAssistedUser() != null
                        ? message.getAssistedUser().getId()
                        : null)

                .assistedFamilyName(message.getAssistedFamilyName())

                .messageType(message.getMessageType())
                .content(resolveCustomerMessageContent(message))
                .mediaFileId(message.getMediaFile() != null
                        ? message.getMediaFile().getId()
                        : null)

                .mediaType(message.getMediaFile() != null
                        ? message.getMediaFile().getMediaType().name()
                        : null)
                .mediaPreviewUrl(
                        message.getMediaFile() != null
                                ? "/api/v1/chat/rooms/"
                                + message.getRoom().getId()
                                + "/messages/"
                                + message.getId()
                                + "/media/"
                                + message.getMediaFile().getId()
                                + "/view"
                                : null
                )
                .fileName(
                        message.getMediaFile() != null
                                ? message.getMediaFile().getOriginalFileName()
                                : null
                )
                .fileSizeBytes(
                        message.getMediaFile() != null
                                ? message.getMediaFile().getFileSizeBytes()
                                : null
                )
                .contentType(
                        message.getMediaFile() != null
                                ? message.getMediaFile().getContentType()
                                : null
                )
                .replyToMessageId(reply != null ? reply.getId() : null)
                .replyPreview(reply != null ? preview(reply.getContent()) : null)
                .deliveryStatus(message.getDeliveryStatus())
                .mine(message.getSenderUser() != null
                        && message.getSenderUser().getId().equals(currentUserId))
                .edited(message.getEditedAt() != null)
                .deleted(message.getDeletedAt() != null)
                .sentAt(message.getSentAt())
                .deliveredAt(message.getDeliveredAt())
                .readAt(message.getReadAt())
                .editedAt(message.getEditedAt())
                .deletedAt(message.getDeletedAt())
                .build();
    }

    private String preview(String content) {
        if (content == null) return null;
        return content.length() <= 80 ? content : content.substring(0, 80) + "...";
    }

    private UUID getOtherUserId(FamilyChatRoom room, UUID currentUserId) {
        if (room.getBoyUser().getId().equals(currentUserId)) {
            return room.getGirlUser().getId();
        }

        return room.getBoyUser().getId();
    }

    private final Map<UUID, Instant> lastMessageMap = new HashMap<>();

    private void validateRateLimit(UUID userId) {

        Instant last = lastMessageMap.get(userId);

        if (last != null && last.plusSeconds(1).isAfter(Instant.now())) {
            throw new IllegalArgumentException("You are sending messages too quickly");
        }

        lastMessageMap.put(userId, Instant.now());
    }

    @Transactional
    public void blockRoom(UUID roomId) {

        UUID userId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, userId);

        if (room.isBlocked()) {
            return;
        }

        UserAccount blocker = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        room.setBlocked(true);
        room.setBlockedByUser(blocker);
        room.setStatus(ChatRoomStatus.BLOCKED);

        /*
         * Admin monitor read-model support
         */
        room.setNeedsAttention(true);
        room.setLastReportReason("Blocked by family");
        room.setUpdatedAt(Instant.now());

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_ROOM_BLOCKED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room blocked"
        );
    }
    @Transactional
    public void closeRoom(UUID roomId) {

        UUID userId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, userId);

        room.setStatus(ChatRoomStatus.CLOSED_BY_ADMIN);
        room.setClosedAt(Instant.now());
        room.setNeedsAttention(false);

        chatRoomRepository.save(room);
        auditLogService.record(
                AuditAction.CHAT_ROOM_CLOSED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room closed"
        );
    }

    @Transactional
    public void reportMessage(
            UUID roomId,
            UUID messageId,
            ReportChatMessageRequest request
    ) {

        UUID userId = AuthUser.getCurrentUserId();

        getAuthorizedRoom(roomId, userId);

        FamilyChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (!message.getRoom().getId().equals(roomId)) {
            throw new IllegalArgumentException("Message does not belong to this room");
        }

        UserAccount reporter = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Reporter not found"));

        ChatMessageReport report = new ChatMessageReport();
        report.setMessage(message);
        report.setReporterUser(reporter);
        report.setReason(request.getReason());
        report.setDetails(request.getDetails());

        reportRepository.save(report);

        message.setModerationStatus(ChatModerationStatus.UNDER_REVIEW);

        FamilyChatRoom room = message.getRoom();
        room.setReported(true);
        room.setNeedsAttention(true);
        room.setStatus(ChatRoomStatus.REPORTED);
        room.setLastReportReason(request.getReason());
        chatRoomRepository.save(room);


        chatMessageRepository.save(message);
        auditLogService.record(
                AuditAction.CHAT_MESSAGE_REPORTED,
                AuditEntityType.CHAT_MESSAGE,
                message.getId(),
                "Chat message reported: " + request.getReason()
        );

        adminChatMonitorEventPublisher.publish(
                AdminChatMonitorEvent.builder()
                        .event(ChatSocketEventType.ADMIN_ROOM_NEEDS_ATTENTION)
                        .roomId(room.getId())
                        .proposalId(room.getProposal() != null ? room.getProposal().getId() : null)
                        .crmCaseId(room.getCrmCase() != null ? room.getCrmCase().getId() : null)
                        .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                        .title("Chat needs attention")
                        .message("A family reported a message.")
                        .emittedAt(Instant.now())
                        .build()
        );
    }

    public void sendTyping(UUID roomId, UUID senderUserId) {

        publishRoomEvent(
                roomId,
                ChatSocketEventType.TYPING,
                Map.of(
                        "roomId", roomId,
                        "senderUserId", senderUserId
                )
        );
    }

    private void updateRoomReadModelAfterMessage(
            FamilyChatRoom room,
            FamilyChatMessage message,
            UserAccount sender
    ) {
        room.setLastMessageText(preview(message.getContent()));
        room.setLastMessageType(message.getMessageType());
        room.setLastMessageAt(message.getSentAt());
        room.setLastMessageByName(resolveSenderDisplayName(sender));
        room.setMessageCount(room.getMessageCount() + 1);
        room.setUpdatedAt(Instant.now());

        if (room.getStatus() == ChatRoomStatus.PENDING_RESPONSE
                || room.getStatus() == ChatRoomStatus.NEEDS_CRM_ATTENTION) {
            room.setStatus(ChatRoomStatus.ACTIVE);
            room.setNeedsAttention(false);
        }
    }

    private String resolveSenderDisplayName(UserAccount sender) {
        return userProfileRepository.findByUserAccountId(sender.getId())
                .map(UserProfile::getCandidateFirstName)
                .filter(name -> name != null && !name.isBlank())
                .orElse("Family");
    }

    private boolean visibleToCustomer(FamilyChatMessage message) {
        if (message.getDeletedAt() != null) {
            return true; // show "This message was deleted."
        }

        if (message.getModerationStatus() == ChatModerationStatus.HIDDEN
                || message.getModerationStatus() == ChatModerationStatus.DELETED) {
            return false;
        }

        return true;
    }

    private String resolveCustomerMessageContent(FamilyChatMessage message) {
        if (message.getDeletedAt() != null) {
            return "This message was deleted.";
        }

        if (message.getModerationStatus() == ChatModerationStatus.HIDDEN
                || message.getModerationStatus() == ChatModerationStatus.DELETED) {
            return "This message is not available.";
        }

        return message.getContent();
    }

    @Transactional
    public int backfillChatRoomBusinessLinks() {
        List<FamilyChatRoom> rooms = chatRoomRepository.findAll();

        int updated = 0;

        for (FamilyChatRoom room : rooms) {
            boolean changed = false;

            RishtaRequest rishtaRequest = room.getRishtaRequest();

            if (rishtaRequest == null) {
                continue;
            }

            if (room.getFromUser() == null && rishtaRequest.getSenderUser() != null) {
                room.setFromUser(rishtaRequest.getSenderUser());
                changed = true;
            }

            if (room.getToUser() == null && rishtaRequest.getReceiverUser() != null) {
                room.setToUser(rishtaRequest.getReceiverUser());
                changed = true;
            }

            if (room.getFromProfile() == null && rishtaRequest.getSenderProfile() != null) {
                room.setFromProfile(rishtaRequest.getSenderProfile());
                changed = true;
            }

            if (room.getToProfile() == null && rishtaRequest.getReceiverProfile() != null) {
                room.setToProfile(rishtaRequest.getReceiverProfile());
                changed = true;
            }

            UUID beforeProposalId = room.getProposal() != null ? room.getProposal().getId() : null;
            UUID beforeCrmCaseId = room.getCrmCase() != null ? room.getCrmCase().getId() : null;
            UUID beforeAssignedEmployeeId = room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null;

            linkProposalAndCrm(room, rishtaRequest);

            UUID afterProposalId = room.getProposal() != null ? room.getProposal().getId() : null;
            UUID afterCrmCaseId = room.getCrmCase() != null ? room.getCrmCase().getId() : null;
            UUID afterAssignedEmployeeId = room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null;

            if (!Objects.equals(beforeProposalId, afterProposalId)
                    || !Objects.equals(beforeCrmCaseId, afterCrmCaseId)
                    || !Objects.equals(beforeAssignedEmployeeId, afterAssignedEmployeeId)) {
                changed = true;
            }

            if (changed) {
                chatRoomRepository.save(room);
                updated++;
            }
        }

        return updated;
    }

    private void publishRoomEvent(
            UUID roomId,
            String event,
            Object payload
    ) {
        messagingTemplate.convertAndSend(
                "/topic/chat.room." + roomId,
                ChatWebSocketEvent.builder()
                        .event(event)
                        .roomId(roomId)
                        .payload(payload)
                        .emittedAt(Instant.now())
                        .build()
        );
    }

    public ChatPresenceResponse getPresence(
            UUID userId
    ) {
        return ChatPresenceResponse.builder()
                .userId(userId)
                .online(
                        chatPresenceService.isOnline(userId)
                )
                .lastSeenAt(
                        chatPresenceService.getLastSeen(userId)
                )
                .build();
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> viewChatMedia(
            UUID roomId,
            UUID messageId,
            UUID mediaId
    ) {
        UUID currentUserId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, currentUserId);

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

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.getContentType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + media.getOriginalFileName() + "\""
                )
                .body(bytes);
    }
    private void assertRoomParticipant(
            FamilyChatRoom room,
            UUID userId
    ) {
        boolean participant =
                room.getBoyUser().getId().equals(userId)
                        || room.getGirlUser().getId().equals(userId);

        if (!participant) {
            throw new AccessDeniedException(
                    "You are not allowed to access this chat room"
            );
        }
    }

}