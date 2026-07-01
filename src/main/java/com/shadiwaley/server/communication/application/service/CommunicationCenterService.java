package com.shadiwaley.server.communication.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.audit.infrastructure.entity.AuditLog;
import com.shadiwaley.server.audit.infrastructure.repository.AuditLogRepository;
import com.shadiwaley.server.chat.domain.ChatMode;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.infrastructure.entity.FamilyChatRoom;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatMessageRepository;
import com.shadiwaley.server.chat.infrastructure.repository.FamilyChatRoomRepository;
import com.shadiwaley.server.communication.domain.CommunicationActivityType;
import com.shadiwaley.server.communication.domain.CommunicationCenterEventType;
import com.shadiwaley.server.communication.domain.CommunicationQueueType;
import com.shadiwaley.server.communication.dto.request.BulkCommunicationActionRequest;
import com.shadiwaley.server.communication.dto.response.*;
import com.shadiwaley.server.communication.dto.websocket.CommunicationCenterEvent;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.revenue.domain.SubscriptionStatus;
import com.shadiwaley.server.revenue.infrastructure.entity.FamilySubscription;
import com.shadiwaley.server.revenue.infrastructure.repository.FamilySubscriptionRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.support.domain.SupportTicketPriority;
import jakarta.persistence.criteria.Predicate;

import java.util.*;

import com.shadiwaley.server.support.domain.SupportTicketStatus;
import com.shadiwaley.server.support.infrastructure.entity.SupportTicket;
import com.shadiwaley.server.support.infrastructure.repository.SupportTicketRepository;
import com.shadiwaley.server.support.infrastructure.repository.SupportTicketReplyRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CommunicationCenterService {

    private final EmployeeAccountRepository employeeAccountRepository;
    private final FamilyChatRoomRepository chatRoomRepository;
    private final FamilyChatMessageRepository chatMessageRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final SupportTicketReplyRepository supportTicketReplyRepository;
    private final FamilySubscriptionRepository familySubscriptionRepository;

    private final ParentProfileRepository parentProfileRepository;
    private final AuditLogService auditLogService;
    private final CommunicationCenterEventPublisher communicationCenterEventPublisher;

    private final AuditLogRepository auditLogRepository;

    @Transactional(readOnly = true)
    public CommunicationDashboardResponse getDashboard() {
        EmployeeAccount actor = getCurrentEmployee();

        Instant startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        Instant endOfToday = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant();

        long activeChatRooms = chatRoomRepository.countByStatus(ChatRoomStatus.ACTIVE);
        long reportedChats = chatRoomRepository.countByReportedTrue();
        long needsAttention = chatRoomRepository.countByNeedsAttentionTrue();

        long openSupportTickets = supportTicketRepository.countByStatus(SupportTicketStatus.OPEN);
        long waitingCustomerTickets = supportTicketRepository.countByStatus(SupportTicketStatus.WAITING_CUSTOMER);

        long unassignedChats = chatRoomRepository.countByAssignedEmployeeIsNull();
        long unassignedTickets = supportTicketRepository.countByAssignedEmployeeIsNull();

        long myAssignedChats = chatRoomRepository.countByAssignedEmployeeId(actor.getId());
        long myAssignedTickets = supportTicketRepository.countByAssignedEmployeeId(actor.getId());

        long crmAssistedChats = chatRoomRepository.countByChatMode(ChatMode.CRM_ASSISTED)
                + chatRoomRepository.countByChatMode(ChatMode.CRM_TO_CRM);

        return CommunicationDashboardResponse.builder()
                .totalOpenItems(activeChatRooms + openSupportTickets)
                .myAssignedItems(myAssignedChats + myAssignedTickets)
                .unassignedItems(unassignedChats + unassignedTickets)
                .needsAttentionItems(needsAttention)
                .reportedChats(reportedChats)
                .openSupportTickets(openSupportTickets)
                .waitingCustomerTickets(waitingCustomerTickets)
                .activeChatRooms(activeChatRooms)
                .crmAssistedChats(crmAssistedChats)
                .premiumFamilies(countActivePlan("PREMIUM"))
                .eliteFamilies(countActivePlan("ELITE"))
                .messagesToday(chatMessageRepository.countBySentAtBetween(startOfToday, endOfToday))
                .supportRepliesToday(supportTicketReplyRepository.countByCreatedAtBetween(startOfToday, endOfToday))
                .build();
    }

    @Transactional(readOnly = true)
    public CommunicationQueueResponse getMyQueue(
            CommunicationQueueType queueType,
            int page,
            int size
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        return switch (queueType) {

            case ASSIGNED_TO_ME -> getAssignedToMeQueue(actor, queueType, pageable);

            case UNASSIGNED -> {
                assertAdmin(actor);
                yield getUnassignedQueue(queueType, pageable);
            }

            case NEEDS_ATTENTION -> getNeedsAttentionQueue(actor, queueType, pageable);

            case REPORTED_CHATS -> getReportedChatsQueue(actor, queueType, pageable);

            case OPEN_SUPPORT_TICKETS -> getOpenSupportTicketsQueue(actor, queueType, pageable);

            case WAITING_CUSTOMER -> getWaitingCustomerQueue(actor, queueType, pageable);

            case CRM_ASSISTED_CHATS -> getCrmAssistedChatsQueue(actor, queueType, pageable);

            case PREMIUM_FAMILIES, ELITE_FAMILIES -> emptyQueue(queueType, safePage, safeSize);
        };
    }

    private CommunicationQueueResponse getAssignedToMeQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<FamilyChatRoom> chatPage =
                chatRoomRepository.findByAssignedEmployeeIdOrderByUpdatedAtDesc(
                        actor.getId(),
                        pageable
                );

        Page<SupportTicket> ticketPage =
                supportTicketRepository.findByAssignedEmployeeIdOrderByUpdatedAtDesc(
                        actor.getId(),
                        pageable
                );

        List<CommunicationQueueItemResponse> items =
                Stream.concat(
                                chatPage.getContent().stream().map(room ->
                                        toChatQueueItem(queueType, room)
                                ),
                                ticketPage.getContent().stream().map(ticket ->
                                        toSupportQueueItem(queueType, ticket)
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        CommunicationQueueItemResponse::getLastActivityAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                        )
                        .limit(pageable.getPageSize())
                        .toList();

        long total =
                chatPage.getTotalElements()
                        + ticketPage.getTotalElements();

        return pageResponse(queueType, items, pageable, total);
    }

    private CommunicationQueueResponse getUnassignedQueue(
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<FamilyChatRoom> chatPage =
                chatRoomRepository.findByAssignedEmployeeIsNullOrderByUpdatedAtDesc(pageable);

        Page<SupportTicket> ticketPage =
                supportTicketRepository.findByAssignedEmployeeIsNullOrderByUpdatedAtDesc(pageable);

        List<CommunicationQueueItemResponse> items =
                Stream.concat(
                                chatPage.getContent().stream().map(room ->
                                        toChatQueueItem(queueType, room)
                                ),
                                ticketPage.getContent().stream().map(ticket ->
                                        toSupportQueueItem(queueType, ticket)
                                )
                        )
                        .sorted(
                                Comparator.comparing(
                                        CommunicationQueueItemResponse::getLastActivityAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                        )
                        .limit(pageable.getPageSize())
                        .toList();

        long total =
                chatPage.getTotalElements()
                        + ticketPage.getTotalElements();

        return pageResponse(queueType, items, pageable, total);
    }

    private CommunicationQueueResponse getNeedsAttentionQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<FamilyChatRoom> chatPage =
                chatRoomRepository.findByNeedsAttentionTrueOrderByUpdatedAtDesc(pageable);

        List<CommunicationQueueItemResponse> items =
                chatPage.getContent()
                        .stream()
                        .filter(room -> canSeeChatRoom(actor, room))
                        .map(room -> toChatQueueItem(queueType, room))
                        .toList();

        return pageResponse(queueType, items, pageable, chatPage.getTotalElements());
    }

    private CommunicationQueueResponse getReportedChatsQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<FamilyChatRoom> chatPage =
                chatRoomRepository.findByReportedTrueOrderByUpdatedAtDesc(pageable);

        List<CommunicationQueueItemResponse> items =
                chatPage.getContent()
                        .stream()
                        .filter(room -> canSeeChatRoom(actor, room))
                        .map(room -> toChatQueueItem(queueType, room))
                        .toList();

        return pageResponse(queueType, items, pageable, chatPage.getTotalElements());
    }

    private CommunicationQueueResponse getOpenSupportTicketsQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<SupportTicket> ticketPage =
                supportTicketRepository.findByStatusOrderByUpdatedAtDesc(
                        SupportTicketStatus.OPEN,
                        pageable
                );

        List<CommunicationQueueItemResponse> items =
                ticketPage.getContent()
                        .stream()
                        .filter(ticket -> canSeeSupportTicket(actor, ticket))
                        .map(ticket -> toSupportQueueItem(queueType, ticket))
                        .toList();

        return pageResponse(queueType, items, pageable, ticketPage.getTotalElements());
    }

    private CommunicationQueueResponse getWaitingCustomerQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<SupportTicket> ticketPage =
                supportTicketRepository.findByStatusOrderByUpdatedAtDesc(
                        SupportTicketStatus.WAITING_CUSTOMER,
                        pageable
                );

        List<CommunicationQueueItemResponse> items =
                ticketPage.getContent()
                        .stream()
                        .filter(ticket -> canSeeSupportTicket(actor, ticket))
                        .map(ticket -> toSupportQueueItem(queueType, ticket))
                        .toList();

        return pageResponse(queueType, items, pageable, ticketPage.getTotalElements());
    }

    private CommunicationQueueResponse getCrmAssistedChatsQueue(
            EmployeeAccount actor,
            CommunicationQueueType queueType,
            Pageable pageable
    ) {
        Page<FamilyChatRoom> crmAssisted =
                chatRoomRepository.findByChatModeOrderByUpdatedAtDesc(
                        ChatMode.CRM_ASSISTED,
                        pageable
                );

        Page<FamilyChatRoom> crmToCrm =
                chatRoomRepository.findByChatModeOrderByUpdatedAtDesc(
                        ChatMode.CRM_TO_CRM,
                        pageable
                );

        List<CommunicationQueueItemResponse> items =
                Stream.concat(
                                crmAssisted.getContent().stream(),
                                crmToCrm.getContent().stream()
                        )
                        .filter(room -> canSeeChatRoom(actor, room))
                        .map(room -> toChatQueueItem(queueType, room))
                        .sorted(
                                Comparator.comparing(
                                        CommunicationQueueItemResponse::getLastActivityAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                        )
                        .limit(pageable.getPageSize())
                        .toList();

        long total =
                crmAssisted.getTotalElements()
                        + crmToCrm.getTotalElements();

        return pageResponse(queueType, items, pageable, total);
    }

    private CommunicationQueueItemResponse toChatQueueItem(
            CommunicationQueueType queueType,
            FamilyChatRoom room
    ) {
        UserAccount customer =
                room.getFromUser() != null
                        ? room.getFromUser()
                        : room.getBoyUser();

        FamilySubscription subscription = resolveBestSubscription(room);

        return CommunicationQueueItemResponse.builder()
                .queueType(queueType)
                .itemType("CHAT_ROOM")
                .itemId(room.getId())
                .customerUserId(customer != null ? customer.getId() : null)
                .customerName(resolveCustomerName(customer))
                .customerPhone(customer != null ? customer.getPhone() : null)
                .title("Family Chat")
                .subtitle(room.getLastMessageText())
                .status(room.getStatus() != null ? room.getStatus().name() : null)
                .priority(room.isNeedsAttention() || room.isReported() ? "HIGH" : "NORMAL")
                .assignedEmployeeId(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(room.getAssignedEmployee() != null ? room.getAssignedEmployee().getFullName() : null)
                .needsAttention(room.isNeedsAttention())
                .reported(room.isReported())
                .planCode(subscription != null ? subscription.getPlanCode() : null)
                .planName(subscription != null ? subscription.getPlanName() : null)
                .lastActivityAt(room.getUpdatedAt())
                .createdAt(room.getCreatedAt())
                .build();
    }
    private boolean canSeeChatRoom(
            EmployeeAccount actor,
            FamilyChatRoom room
    ) {
        if (isAdmin(actor)) {
            return true;
        }

        return room.getAssignedEmployee() != null
                && room.getAssignedEmployee().getId().equals(actor.getId());
    }

    private boolean canSeeSupportTicket(
            EmployeeAccount actor,
            SupportTicket ticket
    ) {
        if (isAdmin(actor)) {
            return true;
        }

        return ticket.getAssignedEmployee() != null
                && ticket.getAssignedEmployee().getId().equals(actor.getId());
    }

    private void assertAdmin(EmployeeAccount actor) {
        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Only admin can access this queue");
        }
    }


    private CommunicationQueueItemResponse toSupportQueueItem(
            CommunicationQueueType queueType,
            SupportTicket ticket
    ) {
        UserAccount customer = ticket.getCustomerUser();

        FamilySubscription subscription =
                customer != null
                        ? getCurrentSubscription(customer.getId())
                        : null;

        return CommunicationQueueItemResponse.builder()
                .queueType(queueType)
                .itemType("SUPPORT_TICKET")
                .itemId(ticket.getId())
                .customerUserId(customer != null ? customer.getId() : null)
                .customerName(resolveCustomerName(customer))
                .customerPhone(customer != null ? customer.getPhone() : null)
                .title(ticket.getSubject())
                .subtitle(ticket.getLastMessage())
                .status(ticket.getStatus() != null ? ticket.getStatus().name() : null)
                .priority(ticket.getPriority() != null ? ticket.getPriority().name() : null)
                .assignedEmployeeId(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getFullName() : null)
                .needsAttention(false)
                .reported(false)
                .planCode(subscription != null ? subscription.getPlanCode() : null)
                .planName(subscription != null ? subscription.getPlanName() : null)
                .lastActivityAt(ticket.getUpdatedAt())
                .createdAt(ticket.getCreatedAt())
                .build();
    }

    private CommunicationQueueResponse pageResponse(
            CommunicationQueueType queueType,
            List<CommunicationQueueItemResponse> items,
            Pageable pageable,
            long total
    ) {
        int totalPages =
                total == 0
                        ? 0
                        : (int) Math.ceil((double) total / pageable.getPageSize());

        return CommunicationQueueResponse.builder()
                .queueType(queueType)
                .items(items)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(total)
                .totalPages(totalPages)
                .last(pageable.getPageNumber() + 1 >= totalPages)
                .build();
    }

    private CommunicationQueueResponse emptyQueue(
            CommunicationQueueType queueType,
            int page,
            int size
    ) {
        return CommunicationQueueResponse.builder()
                .queueType(queueType)
                .items(java.util.List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }
    @Transactional(readOnly = true)
    public CommunicationActivityPageResponse getActivity(
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<AuditLog> result =
                auditLogRepository.findByEntityTypeInOrderByCreatedAtDesc(
                        List.of(
                                AuditEntityType.CHAT_ROOM,
                                AuditEntityType.CHAT_MESSAGE,
                                AuditEntityType.SUPPORT_TICKET,
                                AuditEntityType.CRM_CASE,
                                AuditEntityType.CRM_NOTE,
                                AuditEntityType.CRM_FOLLOW_UP
                        ),
                        pageable
                );

        return CommunicationActivityPageResponse.builder()
                .items(
                        result.getContent()
                                .stream()
                                .map(this::toActivityResponse)
                                .toList()
                )
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    private CommunicationActivityResponse toActivityResponse(AuditLog log) {
        return CommunicationActivityResponse.builder()
                .activityId(log.getId())
                .activityType(resolveActivityType(log))
                .itemType(log.getEntityType() != null ? log.getEntityType().name() : null)
                .itemId(log.getEntityId())
                .actorId(log.getActorId())
                .actorType(log.getActorType())
                .actorName(log.getActorName())
                .title(resolveActivityTitle(log))
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }


    private CommunicationActivityType resolveActivityType(AuditLog log) {
        if (log.getAction() == null) {
            return CommunicationActivityType.SYSTEM;
        }

        return switch (log.getAction()) {
            case CHAT_MESSAGE_SENT_BY_CRM -> CommunicationActivityType.CHAT_MESSAGE;
            case CHAT_ROOM_REPORTED -> CommunicationActivityType.CHAT_REPORTED;
            case CHAT_ROOM_BLOCKED -> CommunicationActivityType.CHAT_BLOCKED;
            case CHAT_ROOM_CLOSED -> CommunicationActivityType.CHAT_CLOSED;
            case CHAT_ASSIGNMENT_UPDATED -> CommunicationActivityType.CHAT_ASSIGNED;

            case SUPPORT_TICKET_CREATED -> CommunicationActivityType.SUPPORT_TICKET_CREATED;
            case SUPPORT_TICKET_REPLY_ADDED -> CommunicationActivityType.SUPPORT_TICKET_REPLIED;
            case SUPPORT_TICKET_ASSIGNED -> CommunicationActivityType.SUPPORT_TICKET_ASSIGNED;
            case SUPPORT_TICKET_STATUS_CHANGED -> CommunicationActivityType.SUPPORT_TICKET_STATUS_CHANGED;

            case CRM_NOTE_ADDED -> CommunicationActivityType.CRM_NOTE_ADDED;
            case CRM_FOLLOW_UP_CREATED -> CommunicationActivityType.CRM_FOLLOW_UP_CREATED;

            default -> CommunicationActivityType.SYSTEM;
        };
    }

    private String resolveActivityTitle(AuditLog log) {
        if (log.getAction() == null) {
            return "System activity";
        }

        return switch (log.getAction()) {
            case CHAT_MESSAGE_SENT_BY_CRM -> "CRM message sent";
            case CHAT_ROOM_REPORTED -> "Chat room reported";
            case CHAT_ROOM_BLOCKED -> "Chat room blocked";
            case CHAT_ROOM_CLOSED -> "Chat room closed";
            case CHAT_ASSIGNMENT_UPDATED -> "Chat room assigned";

            case SUPPORT_TICKET_CREATED -> "Support ticket created";
            case SUPPORT_TICKET_REPLY_ADDED -> "Support ticket replied";
            case SUPPORT_TICKET_ASSIGNED -> "Support ticket assigned";
            case SUPPORT_TICKET_STATUS_CHANGED -> "Support ticket status changed";

            case CRM_NOTE_ADDED -> "CRM note added";
            case CRM_FOLLOW_UP_CREATED -> "CRM follow-up created";

            default -> "Communication activity";
        };
    }

    private EmployeeAccount getCurrentEmployee() {
        return employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }

    private long countActivePlan(String planCode) {
        return familySubscriptionRepository
                .countByPlanCodeAndSubscriptionStatus(
                        planCode,
                        com.shadiwaley.server.revenue.domain.SubscriptionStatus.ACTIVE
                );
    }

    private boolean isAdmin(EmployeeAccount employee) {
        return employee.getRole() == EmployeeRole.SUPER_ADMIN
                || employee.getRole() == EmployeeRole.ADMIN;
    }

    private String resolveCustomerName(UserAccount user) {
        if (user == null) {
            return "Unknown Family";
        }

        return parentProfileRepository.findByUserAccountId(user.getId())
                .map(ParentProfile::getParentName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(user.getPhone());
    }

    private FamilySubscription getCurrentSubscription(UUID userId) {
        if (userId == null) {
            return null;
        }

        return familySubscriptionRepository
                .findFirstByUserAccountIdAndCurrentSubscriptionTrue(userId)
                .orElse(null);
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

        if (isEligibleCrmPlan(boySubscription)) {
            return boySubscription;
        }

        if (isEligibleCrmPlan(girlSubscription)) {
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

    private boolean isActiveSubscription(FamilySubscription subscription) {
        return subscription != null
                && subscription.getSubscriptionStatus() == SubscriptionStatus.ACTIVE;
    }

    private boolean isEligibleCrmPlan(FamilySubscription subscription) {
        if (!isActiveSubscription(subscription)) {
            return false;
        }

        return switch (subscription.getPlanCode()) {
            case "PREMIUM", "ELITE" -> true;
            default -> false;
        };
    }

    @Transactional(readOnly = true)
    public CommunicationSearchResponse search(
            String query,
            int page,
            int size
    ) {
        EmployeeAccount actor = getCurrentEmployee();

        if (query == null || query.isBlank()) {
            return CommunicationSearchResponse.builder()
                    .query(query)
                    .items(List.of())
                    .page(Math.max(page, 0))
                    .size(Math.min(Math.max(size, 1), 100))
                    .totalElements(0)
                    .totalPages(0)
                    .last(true)
                    .build();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Pageable pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<FamilyChatRoom> chatRooms =
                chatRoomRepository.findAll(
                        buildChatSearchSpec(actor, query),
                        pageable
                );

        Page<SupportTicket> tickets =
                supportTicketRepository.findAll(
                        buildSupportSearchSpec(actor, query),
                        pageable
                );

        List<CommunicationQueueItemResponse> items =
                Stream.concat(
                                chatRooms.getContent()
                                        .stream()
                                        .map(room -> toChatQueueItem(
                                                CommunicationQueueType.ASSIGNED_TO_ME,
                                                room
                                        )),
                                tickets.getContent()
                                        .stream()
                                        .map(ticket -> toSupportQueueItem(
                                                CommunicationQueueType.OPEN_SUPPORT_TICKETS,
                                                ticket
                                        ))
                        )
                        .sorted(
                                Comparator.comparing(
                                        CommunicationQueueItemResponse::getLastActivityAt,
                                        Comparator.nullsLast(Comparator.reverseOrder())
                                )
                        )
                        .limit(safeSize)
                        .toList();

        long total =
                chatRooms.getTotalElements()
                        + tickets.getTotalElements();

        int totalPages =
                total == 0
                        ? 0
                        : (int) Math.ceil((double) total / safeSize);

        return CommunicationSearchResponse.builder()
                .query(query)
                .items(items)
                .page(safePage)
                .size(safeSize)
                .totalElements(total)
                .totalPages(totalPages)
                .last(safePage + 1 >= totalPages)
                .build();
    }

    private Specification<FamilyChatRoom> buildChatSearchSpec(
            EmployeeAccount actor,
            String query
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin(actor)) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), actor.getId())
                );
            }

            String pattern = "%" + query.trim().toLowerCase() + "%";

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

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<SupportTicket> buildSupportSearchSpec(
            EmployeeAccount actor,
            String query
    ) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin(actor)) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), actor.getId())
                );
            }

            String pattern = "%" + query.trim().toLowerCase() + "%";

            predicates.add(
                    cb.or(
                            cb.like(cb.lower(root.get("subject")), pattern),
                            cb.like(cb.lower(root.get("lastMessage")), pattern),
                            cb.like(cb.lower(root.get("customerUser").get("phone")), pattern)
                    )
            );

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public BulkCommunicationActionResponse bulkAction(
            BulkCommunicationActionRequest request
    ) {
        EmployeeAccount actor = getCurrentEmployee();
        assertAdmin(actor);

        int success = 0;
        int failed = 0;

        for (UUID id : request.getItemIds()) {
            try {
                processBulkItem(id, request);
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.SYSTEM,
                null,
                "Communication center bulk action completed: "
                        + request.getAction()
                        + ", success="
                        + success
                        + ", failed="
                        + failed
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.BULK_ACTION_COMPLETED,
                "BULK_ACTION",
                null,
                null,
                actor.getId(),
                "Bulk action completed",
                "Communication center bulk action completed."
        );

        return BulkCommunicationActionResponse.builder()
                .processed(request.getItemIds().size())
                .success(success)
                .failed(failed)
                .build();
    }


    private void processBulkItem(
            UUID id,
            BulkCommunicationActionRequest request
    ) {
        Optional<SupportTicket> ticketOptional =
                supportTicketRepository.findById(id);

        if (ticketOptional.isPresent()) {
            processSupportBulkItem(ticketOptional.get(), request);
            return;
        }

        Optional<FamilyChatRoom> roomOptional =
                chatRoomRepository.findById(id);

        if (roomOptional.isPresent()) {
            processChatBulkItem(roomOptional.get(), request);
            return;
        }

        throw new EntityNotFoundException("Communication item not found");
    }

    private void processSupportBulkItem(
            SupportTicket ticket,
            BulkCommunicationActionRequest request
    ) {
        switch (request.getAction()) {
            case ASSIGN -> assignSupportTicket(ticket, request.getEmployeeId());

            case CHANGE_PRIORITY -> changeSupportPriority(ticket, request.getPriority());

            case CHANGE_STATUS -> changeSupportStatus(ticket, request.getStatus());

            case CLOSE -> closeSupport(ticket);

            case RESOLVE -> resolveSupport(ticket);

            default -> throw new IllegalArgumentException(
                    "Action not supported for support ticket"
            );
        }
    }

    private void processChatBulkItem(
            FamilyChatRoom room,
            BulkCommunicationActionRequest request
    ) {
        switch (request.getAction()) {
            case ASSIGN -> assignChat(room, request.getEmployeeId());

            case MARK_NEEDS_ATTENTION -> markAttention(room);

            case REMOVE_NEEDS_ATTENTION -> removeAttention(room);

            case CLOSE -> closeChat(room);

            default -> throw new IllegalArgumentException(
                    "Action not supported for chat room"
            );
        }
    }

    private void assignSupportTicket(
            SupportTicket ticket,
            UUID employeeId
    ) {
        EmployeeAccount employee = validateAssignableEmployee(employeeId);

        ticket.setAssignedEmployee(employee);

        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }

        supportTicketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_ASSIGNED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket assigned from communication center"
        );

        publishCommunicationEvent(
                CommunicationCenterEventType.SUPPORT_TICKET_ASSIGNED,
                "SUPPORT_TICKET",
                ticket.getId(),
                ticket.getCustomerUser() != null ? ticket.getCustomerUser().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                "Support ticket assigned",
                "A support ticket was assigned."
        );
    }

    private void assignChat(
            FamilyChatRoom room,
            UUID employeeId
    ) {
        EmployeeAccount employee = validateAssignableEmployee(employeeId);

        room.setAssignedEmployee(employee);

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_ASSIGNMENT_UPDATED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room assigned from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.CHAT_ROOM_ASSIGNED,
                "CHAT_ROOM",
                room.getId(),
                room.getFromUser() != null ? room.getFromUser().getId() : null,
                room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null,
                "Chat room assigned",
                "A chat room was assigned."
        );
    }

    private void closeSupport(
            SupportTicket ticket
    ) {
        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedAt(Instant.now());

        supportTicketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_STATUS_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket closed from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.SUPPORT_TICKET_STATUS_CHANGED,
                "SUPPORT_TICKET",
                ticket.getId(),
                ticket.getCustomerUser() != null ? ticket.getCustomerUser().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                "Support ticket closed",
                "A support ticket was closed."
        );
    }

    private void resolveSupport(
            SupportTicket ticket
    ) {
        ticket.setStatus(SupportTicketStatus.RESOLVED);
        ticket.setResolvedAt(Instant.now());

        supportTicketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_STATUS_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket resolved from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.SUPPORT_TICKET_STATUS_CHANGED,
                "SUPPORT_TICKET",
                ticket.getId(),
                ticket.getCustomerUser() != null ? ticket.getCustomerUser().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                "Support ticket resolved",
                "A support ticket was resolved."
        );
    }

    private void closeChat(
            FamilyChatRoom room
    ) {
        room.setStatus(ChatRoomStatus.CLOSED);
        room.setClosedAt(Instant.now());

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_ROOM_CLOSED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room closed from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.CHAT_ROOM_CLOSED,
                "CHAT_ROOM",
                room.getId(),
                room.getFromUser() != null ? room.getFromUser().getId() : null,
                room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null,
                "Chat room closed",
                "A chat room was closed."
        );
    }

    private void markAttention(
            FamilyChatRoom room
    ) {
        room.setNeedsAttention(true);

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_ROOM_STATUS_UPDATED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room marked as needs attention from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.CHAT_NEEDS_ATTENTION,
                "CHAT_ROOM",
                room.getId(),
                room.getFromUser() != null ? room.getFromUser().getId() : null,
                room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null,
                "Chat needs attention",
                "A chat room was marked as needs attention."
        );
    }

    private void removeAttention(
            FamilyChatRoom room
    ) {
        room.setNeedsAttention(false);

        chatRoomRepository.save(room);

        auditLogService.record(
                AuditAction.CHAT_ROOM_STATUS_UPDATED,
                AuditEntityType.CHAT_ROOM,
                room.getId(),
                "Chat room removed from needs attention from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.CHAT_ATTENTION_REMOVED,
                "CHAT_ROOM",
                room.getId(),
                room.getFromUser() != null ? room.getFromUser().getId() : null,
                room.getAssignedEmployee() != null ? room.getAssignedEmployee().getId() : null,
                "Chat attention removed",
                "A chat room was removed from needs attention."
        );
    }

    private void changeSupportPriority(
            SupportTicket ticket,
            String priority
    ) {
        if (priority == null || priority.isBlank()) {
            throw new IllegalArgumentException("Priority is required");
        }

        SupportTicketPriority parsedPriority =
                SupportTicketPriority.valueOf(priority.trim().toUpperCase());

        ticket.setPriority(parsedPriority);

        supportTicketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_PRIORITY_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket priority changed from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.SUPPORT_TICKET_STATUS_CHANGED,
                "SUPPORT_TICKET",
                ticket.getId(),
                ticket.getCustomerUser() != null ? ticket.getCustomerUser().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                "Support ticket priority changed",
                "A support ticket priority was changed."
        );
    }

    private void changeSupportStatus(
            SupportTicket ticket,
            String status
    ) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }

        SupportTicketStatus parsedStatus =
                SupportTicketStatus.valueOf(status.trim().toUpperCase());

        ticket.setStatus(parsedStatus);

        if (parsedStatus == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(Instant.now());
        }

        if (parsedStatus == SupportTicketStatus.CLOSED) {
            ticket.setClosedAt(Instant.now());
        }

        supportTicketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_STATUS_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket status changed from communication center"
        );
        publishCommunicationEvent(
                CommunicationCenterEventType.SUPPORT_TICKET_STATUS_CHANGED,
                "SUPPORT_TICKET",
                ticket.getId(),
                ticket.getCustomerUser() != null ? ticket.getCustomerUser().getId() : null,
                ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null,
                "Support ticket status changed",
                "A support ticket status was changed."
        );
    }

    private EmployeeAccount validateAssignableEmployee(UUID employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("Employee id is required");
        }

        EmployeeAccount employee = employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Employee is not active");
        }

        if (!List.of(
                EmployeeRole.SUPER_ADMIN,
                EmployeeRole.ADMIN,
                EmployeeRole.CRM_AGENT,
                EmployeeRole.SUPPORT_AGENT
        ).contains(employee.getRole())) {
            throw new IllegalArgumentException("Employee cannot be assigned communication items");
        }

        return employee;
    }

    private void publishCommunicationEvent(
            String event,
            String itemType,
            UUID itemId,
            UUID customerUserId,
            UUID assignedEmployeeId,
            String title,
            String message
    ) {
        communicationCenterEventPublisher.publish(
                CommunicationCenterEvent.builder()
                        .event(event)
                        .itemType(itemType)
                        .itemId(itemId)
                        .customerUserId(customerUserId)
                        .assignedEmployeeId(assignedEmployeeId)
                        .title(title)
                        .message(message)
                        .emittedAt(Instant.now())
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public CommunicationPermissionResponse getPermissions() {
        EmployeeAccount employee = getCurrentEmployee();

        boolean admin = isAdmin(employee);
        boolean crm = employee.getRole() == EmployeeRole.CRM_AGENT;
        boolean support = employee.getRole() == EmployeeRole.SUPPORT_AGENT;

        return CommunicationPermissionResponse.builder()
                .canViewAll(admin)
                .canAssign(admin)
                .canBulkAction(admin)
                .canReplyToChat(admin || crm)
                .canReplyToSupport(admin || support || crm)
                .canModerateChat(admin)
                .canCloseChat(admin || crm)
                .canCloseSupport(admin || support || crm)
                .canViewReports(admin || support)
                .canViewDashboard(true)
                .canExport(admin)
                .build();
    }


}