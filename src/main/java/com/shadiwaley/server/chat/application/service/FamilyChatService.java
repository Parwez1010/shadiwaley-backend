package com.shadiwaley.server.chat.application.service;

import com.shadiwaley.server.chat.domain.*;
import com.shadiwaley.server.chat.dto.request.ReportChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.SendChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.UpdateChatMessageRequest;
import com.shadiwaley.server.chat.dto.response.*;
import com.shadiwaley.server.chat.infrastructure.entity.*;
import com.shadiwaley.server.chat.infrastructure.repository.*;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.rishta.domain.RishtaRequestStatus;
import com.shadiwaley.server.rishta.infrastructure.entity.RishtaRequest;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

    @Transactional
    public FamilyChatRoom createRoomForAcceptedRishta(RishtaRequest rishtaRequest) {
        if (rishtaRequest.getStatus() != RishtaRequestStatus.ACCEPTED) {
            throw new IllegalArgumentException("Chat room can be created only for accepted rishta requests");
        }

        return chatRoomRepository.findByRishtaRequestId(rishtaRequest.getId())
                .orElseGet(() -> {
                    FamilyChatRoom room = new FamilyChatRoom();
                    room.setRishtaRequest(rishtaRequest);

                    if (rishtaRequest.getSenderUser().getSide() == UserSide.BOY) {
                        room.setBoyUser(rishtaRequest.getSenderUser());
                        room.setGirlUser(rishtaRequest.getReceiverUser());
                    } else {
                        room.setBoyUser(rishtaRequest.getReceiverUser());
                        room.setGirlUser(rishtaRequest.getSenderUser());
                    }

                    return chatRoomRepository.save(room);
                });
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
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setMediaFile(media);
        message.setReplyToMessage(replyTo);
        message.setDeliveryStatus(ChatDeliveryStatus.SENT);
        message.setDeliveredAt(Instant.now());

        FamilyChatMessage saved = chatMessageRepository.save(message);

        room.setUpdatedAt(Instant.now());
        chatRoomRepository.save(room);

        UUID receiverUserId = getOtherUserId(room, senderUserId);

        notificationService.create(
                receiverUserId,
                NotificationType.CHAT_OPENED,
                "New family message",
                "You have received a new message from a connected family.",
                "/chat/" + room.getId(),
                room.getId()
        );

        ChatMessageResponse response = toMessageResponse(saved, senderUserId);

        messagingTemplate.convertAndSend(
                "/topic/chat.room." + room.getId(),
                response
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

        messagingTemplate.convertAndSend("/topic/chat.room." + roomId, response);

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

        messagingTemplate.convertAndSend(
                "/topic/chat.room." + roomId,
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

        messagingTemplate.convertAndSend(
                "/topic/chat.room." + roomId + ".read",
                Map.of("roomId", roomId, "readerUserId", userId, "readAt", now)
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

        return ChatRoomResponse.builder()
                .roomId(room.getId())
                .rishtaRequestId(room.getRishtaRequest().getId())
                .otherUserId(otherUserId)
                .otherProfileId(otherProfile.getId())
                .otherDisplayId(otherProfile.getDisplayId())
                .otherName(otherProfile.getCandidateFirstName())
                .status(room.getStatus())
                .lastMessage(lastMessage != null ? lastMessage.getContent() : null)
                .lastMessageAt(lastMessage != null ? lastMessage.getSentAt() : null)
                .unreadCount(unreadCount)
                .createdAt(room.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toMessageResponse(FamilyChatMessage message, UUID currentUserId) {
        UserProfile senderProfile = userProfileRepository.findByUserAccountId(message.getSenderUser().getId())
                .orElse(null);

        FamilyChatMessage reply = message.getReplyToMessage();

        return ChatMessageResponse.builder()
                .messageId(message.getId())
                .roomId(message.getRoom().getId())
                .senderUserId(message.getSenderUser().getId())
                .senderDisplayName(senderProfile != null ? senderProfile.getCandidateFirstName() : "Family")
                .messageType(message.getMessageType())
                .content(message.getContent())
                .mediaFileId(message.getMediaFile() != null
                        ? message.getMediaFile().getId()
                        : null)

                .mediaType(message.getMediaFile() != null
                        ? message.getMediaFile().getMediaType().name()
                        : null)
                .replyToMessageId(reply != null ? reply.getId() : null)
                .replyPreview(reply != null ? preview(reply.getContent()) : null)
                .deliveryStatus(message.getDeliveryStatus())
                .mine(message.getSenderUser().getId().equals(currentUserId))
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

        chatRoomRepository.save(room);
    }

    @Transactional
    public void closeRoom(UUID roomId) {

        UUID userId = AuthUser.getCurrentUserId();

        FamilyChatRoom room = getAuthorizedRoom(roomId, userId);

        room.setStatus(ChatRoomStatus.CLOSED);
        room.setClosedAt(Instant.now());

        chatRoomRepository.save(room);
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

        chatMessageRepository.save(message);
    }

    public void sendTyping(UUID roomId, UUID senderUserId) {

        messagingTemplate.convertAndSend(
                "/topic/chat.room." + roomId + ".typing",
                java.util.Map.of(
                        "roomId", roomId,
                        "senderUserId", senderUserId
                )
        );
    }


}