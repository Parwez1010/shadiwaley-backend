package com.shadiwaley.server.support.application;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.media.infrastructure.entity.MediaFile;
import com.shadiwaley.server.media.infrastructure.repository.MediaFileRepository;
import com.shadiwaley.server.notification.application.service.NotificationService;
import com.shadiwaley.server.notification.domain.NotificationType;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.support.domain.*;
import com.shadiwaley.server.support.dto.request.*;
import com.shadiwaley.server.support.dto.response.*;
import com.shadiwaley.server.support.infrastructure.entity.*;
import com.shadiwaley.server.support.infrastructure.repository.*;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import jakarta.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketReplyRepository replyRepository;
    private final UserAccountRepository userAccountRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final MediaFileRepository mediaFileRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    private final CrmCaseRepository crmCaseRepository;
    private final UserProfileRepository userProfileRepository;


    private final EmployeeAccountRepository employeeAccountRepository;

    @Transactional
    public SupportTicketDetailResponse createTicket(CreateSupportTicketRequest request) {
        UUID userId = AuthUser.getCurrentUserId();

        UserAccount customer = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Customer account not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setCustomerUser(customer);
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setSubject(request.getSubject());
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setAssignedEmployee(resolveAssignedEmployee(userId, null));
        ticket.setLastMessage(preview(request.getMessage()));
        ticket.setLastRepliedAt(java.time.Instant.now());

        SupportTicket savedTicket = ticketRepository.save(ticket);

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(savedTicket);
        reply.setSenderType(SupportReplySenderType.CUSTOMER);
        reply.setSenderUser(customer);
        reply.setMessage(request.getMessage());
        reply.setInternalNote(false);

        replyRepository.save(reply);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_CREATED,
                AuditEntityType.SUPPORT_TICKET,
                savedTicket.getId(),
                "Customer created support ticket"
        );

        return getMyTicketDetail(savedTicket.getId());
    }

    @Transactional(readOnly = true)
    public SupportTicketPageResponse getMyTickets(
            int page,
            int size,
            SupportTicketStatus status
    ) {
        UUID userId = AuthUser.getCurrentUserId();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<SupportTicket> result = status == null
                ? ticketRepository.findByCustomerUserIdOrderByUpdatedAtDesc(userId, pageable)
                : ticketRepository.findByCustomerUserIdAndStatusOrderByUpdatedAtDesc(userId, status, pageable);

        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getMyTicketDetail(UUID ticketId) {
        UUID userId = AuthUser.getCurrentUserId();

        SupportTicket ticket = ticketRepository.findByIdAndCustomerUserId(ticketId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        List<SupportTicketReply> replies =
                replyRepository.findByTicketIdAndInternalNoteFalseOrderByCreatedAtAsc(ticket.getId());

        return toDetailResponse(ticket, replies);
    }

    @Transactional
    public SupportTicketDetailResponse replyToMyTicket(
            UUID ticketId,
            SupportTicketReplyRequest request
    ) {
        UUID userId = AuthUser.getCurrentUserId();

        SupportTicket ticket = ticketRepository.findByIdAndCustomerUserId(ticketId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        if (ticket.getStatus() == SupportTicketStatus.CLOSED) {
            throw new IllegalArgumentException("Closed ticket cannot be replied to");
        }

        UserAccount customer = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Customer account not found"));

        MediaFile mediaFile = resolveOwnedMedia(request.getMediaFileId(), userId);

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setSenderType(SupportReplySenderType.CUSTOMER);
        reply.setSenderUser(customer);
        reply.setMessage(request.getMessage());
        reply.setMediaFile(mediaFile);
        reply.setInternalNote(false);

        replyRepository.save(reply);

        ticket.setLastMessage(preview(request.getMessage()));
        ticket.setLastRepliedAt(java.time.Instant.now());

        if (ticket.getStatus() == SupportTicketStatus.RESOLVED) {
            ticket.setStatus(SupportTicketStatus.OPEN);
            ticket.setResolvedAt(null);
        } else {
            ticket.setStatus(SupportTicketStatus.OPEN);
        }

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_REPLY_ADDED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Customer replied to support ticket"
        );

        return getMyTicketDetail(ticket.getId());
    }

    @Transactional
    public SupportTicketDetailResponse closeMyTicket(UUID ticketId) {
        UUID userId = AuthUser.getCurrentUserId();

        SupportTicket ticket = ticketRepository.findByIdAndCustomerUserId(ticketId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        ticket.setStatus(SupportTicketStatus.CLOSED);
        ticket.setClosedAt(java.time.Instant.now());

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_CLOSED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Customer closed support ticket"
        );

        return getMyTicketDetail(ticket.getId());
    }

    @Transactional(readOnly = true)
    public SupportOptionsResponse getOptions() {
        return SupportOptionsResponse.builder()
                .categories(Arrays.stream(SupportTicketCategory.values()).map(Enum::name).toList())
                .priorities(Arrays.stream(SupportTicketPriority.values()).map(Enum::name).toList())
                .statuses(Arrays.stream(SupportTicketStatus.values()).map(Enum::name).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public SupportTicketSummaryResponse getMySummary() {
        UUID userId = AuthUser.getCurrentUserId();

        List<SupportTicket> tickets = ticketRepository
                .findByCustomerUserIdOrderByUpdatedAtDesc(
                        userId,
                        PageRequest.of(0, 1000)
                )
                .getContent();

        return buildSummary(tickets);
    }

    private MediaFile resolveOwnedMedia(UUID mediaFileId, UUID userId) {
        if (mediaFileId == null) {
            return null;
        }

        MediaFile media = mediaFileRepository.findByIdAndDeletedFalse(mediaFileId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));

        if (!media.getUserAccount().getId().equals(userId)) {
            throw new AccessDeniedException("You can attach only your own media");
        }

        return media;
    }

    private EmployeeAccount resolveAssignedEmployee(
            UUID customerUserId,
            UUID assignedEmployeeId
    ) {
        if (assignedEmployeeId != null) {
            EmployeeAccount employee = employeeAccountRepository
                    .findById(assignedEmployeeId)
                    .orElseThrow(() -> new EntityNotFoundException("Assigned employee not found"));

            if (employee.getStatus() != EmployeeStatus.ACTIVE) {
                throw new IllegalArgumentException("Assigned employee is not active");
            }

            if (!List.of(
                    EmployeeRole.SUPER_ADMIN,
                    EmployeeRole.ADMIN,
                    EmployeeRole.CRM_AGENT
            ).contains(employee.getRole())) {
                throw new IllegalArgumentException("Employee cannot be assigned support tickets");
            }

            return employee;
        }

        return crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(customerUserId)
                .map(CrmCase::getAssignedEmployee)
                .filter(employee -> employee != null && employee.getStatus() == EmployeeStatus.ACTIVE)
                .orElse(null);
    }

    private SupportTicketPageResponse toPageResponse(Page<SupportTicket> page) {
        return SupportTicketPageResponse.builder()
                .items(page.getContent().stream().map(this::toTicketResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private SupportTicketResponse toTicketResponse(SupportTicket ticket) {
        return SupportTicketResponse.builder()
                .ticketId(ticket.getId())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .source(ticket.getSource())
                .customerUserId(ticket.getCustomerUser().getId())
                .customerName(resolveCustomerName(ticket))
                .customerPhone(ticket.getCustomerUser().getPhone())
                .lastMessage(ticket.getLastMessage())
                .assignedEmployeeId(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getFullName() : null)
                .lastRepliedAt(ticket.getLastRepliedAt())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }


    private SupportTicketDetailResponse toDetailResponse(
            SupportTicket ticket,
            List<SupportTicketReply> replies
    ) {
        UserAccount customer = ticket.getCustomerUser();

        String customerName = parentProfileRepository.findByUserAccountId(customer.getId())
                .map(ParentProfile::getParentName)
                .orElse("Customer");

        return SupportTicketDetailResponse.builder()
                .ticketId(ticket.getId())
                .subject(ticket.getSubject())
                .category(ticket.getCategory())
                .priority(ticket.getPriority())
                .status(ticket.getStatus())
                .source(ticket.getSource())
                .customerUserId(customer.getId())
                .customerName(customerName)
                .customerPhone(customer.getPhone())
                .assignedEmployeeId(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getId() : null)
                .assignedEmployeeName(ticket.getAssignedEmployee() != null ? ticket.getAssignedEmployee().getFullName() : null)
                .createdByEmployeeId(ticket.getCreatedByEmployee() != null ? ticket.getCreatedByEmployee().getId() : null)
                .createdByEmployeeName(ticket.getCreatedByEmployee() != null ? ticket.getCreatedByEmployee().getFullName() : null)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .closedAt(ticket.getClosedAt())
                .replies(replies.stream().map(this::toReplyResponse).toList())
                .build();
    }

    private SupportTicketReplyResponse toReplyResponse(SupportTicketReply reply) {
        String senderName = switch (reply.getSenderType()) {
            case CUSTOMER -> reply.getSenderUser() != null
                    ? parentProfileRepository.findByUserAccountId(reply.getSenderUser().getId())
                    .map(ParentProfile::getParentName)
                    .orElse("Customer")
                    : "Customer";
            case EMPLOYEE -> reply.getSenderEmployee() != null
                    ? reply.getSenderEmployee().getFullName()
                    : "Support";
            case SYSTEM -> "System";
        };

        return SupportTicketReplyResponse.builder()
                .replyId(reply.getId())
                .senderType(reply.getSenderType())
                .senderUserId(reply.getSenderUser() != null ? reply.getSenderUser().getId() : null)
                .senderEmployeeId(reply.getSenderEmployee() != null ? reply.getSenderEmployee().getId() : null)
                .senderName(senderName)
                .message(reply.getMessage())
                .mediaFileId(reply.getMediaFile() != null ? reply.getMediaFile().getId() : null)
                .mediaUrl(reply.getMediaFile() != null ? "/api/v1/media/" + reply.getMediaFile().getId() + "/view" : null)
                .internalNote(reply.isInternalNote())
                .createdAt(reply.getCreatedAt())
                .build();
    }

    private SupportTicketSummaryResponse buildSummary(List<SupportTicket> tickets) {
        return SupportTicketSummaryResponse.builder()
                .totalTickets(tickets.size())
                .openTickets(tickets.stream().filter(t -> t.getStatus() == SupportTicketStatus.OPEN).count())
                .inProgressTickets(tickets.stream().filter(t -> t.getStatus() == SupportTicketStatus.IN_PROGRESS).count())
                .waitingCustomerTickets(tickets.stream().filter(t -> t.getStatus() == SupportTicketStatus.WAITING_CUSTOMER).count())
                .resolvedTickets(tickets.stream().filter(t -> t.getStatus() == SupportTicketStatus.RESOLVED).count())
                .closedTickets(tickets.stream().filter(t -> t.getStatus() == SupportTicketStatus.CLOSED).count())
                .build();
    }

    private String preview(String message) {
        if (message == null) {
            return null;
        }

        return message.length() <= 500 ? message : message.substring(0, 500);
    }

    @Transactional
    public SupportTicketDetailResponse assignTicket(UUID ticketId, UUID employeeId) {
        EmployeeAccount actor = getCurrentEmployee();

        if (!isAdmin(actor)) {
            throw new AccessDeniedException("Only admin can assign support tickets");
        }

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        EmployeeAccount employee = validateAssignableEmployee(employeeId);

        ticket.setAssignedEmployee(employee);

        if (ticket.getStatus() == SupportTicketStatus.OPEN) {
            ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        }

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_ASSIGNED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support ticket assigned"
        );

        notificationService.create(
                ticket.getCustomerUser().getId(),
                NotificationType.SUPPORT_TICKET_ASSIGNED,
                "Support Ticket Assigned",
                "A support agent has been assigned to your ticket.",
                "/support/" + ticket.getId(),
                ticket.getId()
        );

        return getTicketDetailForAdmin(ticket.getId());
    }

    @Transactional
    public SupportTicketDetailResponse updateStatus(
            UUID ticketId,
            SupportTicketStatus status
    ) {

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Support ticket not found"));

        ticket.setStatus(status);

        if (status == SupportTicketStatus.RESOLVED) {
            ticket.setResolvedAt(java.time.Instant.now());
        }

        if (status == SupportTicketStatus.CLOSED) {
            ticket.setClosedAt(java.time.Instant.now());
        }

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_STATUS_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support status updated"
        );

        return getTicketDetailForAdmin(ticketId);
    }

    @Transactional
    public SupportTicketDetailResponse updatePriority(
            UUID ticketId,
            SupportTicketPriority priority
    ) {

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Support ticket not found"));

        ticket.setPriority(priority);

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_PRIORITY_CHANGED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                "Support priority updated"
        );

        return getTicketDetailForAdmin(ticketId);
    }

    @Transactional
    public SupportTicketDetailResponse adminReply(UUID ticketId, SupportTicketReplyRequest request) {
        EmployeeAccount employee = getCurrentEmployee();

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        assertCanAccessTicket(employee, ticket);

        MediaFile mediaFile = null;

        if (request.getMediaFileId() != null) {
            mediaFile = mediaFileRepository.findByIdAndDeletedFalse(request.getMediaFileId())
                    .orElseThrow(() -> new EntityNotFoundException("Attachment not found"));
        }

        boolean internalNote = Boolean.TRUE.equals(request.getInternalNote());

        SupportTicketReply reply = new SupportTicketReply();
        reply.setTicket(ticket);
        reply.setSenderType(SupportReplySenderType.EMPLOYEE);
        reply.setSenderEmployee(employee);
        reply.setMessage(request.getMessage());
        reply.setMediaFile(mediaFile);
        reply.setInternalNote(internalNote);

        replyRepository.save(reply);

        if (!internalNote) {
            if (ticket.getStatus() == SupportTicketStatus.OPEN) {
                ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
            }

            ticket.setLastMessage(preview(request.getMessage()));
            ticket.setLastRepliedAt(Instant.now());

            notificationService.create(
                    ticket.getCustomerUser().getId(),
                    NotificationType.SUPPORT_TICKET_REPLIED,
                    "Support Team Replied",
                    "You have received a reply from support.",
                    "/support/" + ticket.getId(),
                    ticket.getId()
            );
        }

        ticketRepository.save(ticket);

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_REPLY_ADDED,
                AuditEntityType.SUPPORT_TICKET,
                ticket.getId(),
                internalNote ? "Internal support note added" : "Support replied to customer"
        );

        return getTicketDetailForAdmin(ticketId);
    }

    @Transactional(readOnly = true)
    public SupportTicketDetailResponse getTicketDetailForAdmin(UUID ticketId) {
        EmployeeAccount actor = getCurrentEmployee();

        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Support ticket not found"));

        assertCanAccessTicket(actor, ticket);

        List<SupportTicketReply> replies = isAdmin(actor)
                ? replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId)
                : replyRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);

        return toDetailResponse(ticket, replies);
    }


    @Transactional(readOnly = true)
    public SupportTicketPageResponse getAllTickets(
            int page,
            int size,
            SupportTicketStatus status
    ) {

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "updatedAt")
        );

        Page<SupportTicket> result;

        if (status != null) {
            result =
                    ticketRepository.findByStatusOrderByUpdatedAtDesc(
                            status,
                            pageable
                    );
        } else {
            result =
                    ticketRepository.findAllByOrderByUpdatedAtDesc(
                            pageable
                    );
        }

        return toPageResponse(result);
    }
//
//    @Transactional
//    public SupportTicketDetailResponse createTicketFromAdmin(AdminCreateSupportTicketRequest request) {
//        EmployeeAccount actor = getCurrentEmployee();
//
//        UserAccount customer = userAccountRepository.findById(request.getCustomerUserId())
//                .orElseThrow(() -> new EntityNotFoundException("Customer account not found"));
//
//        EmployeeAccount assignee = resolveAssignedEmployee(
//                customer.getId(),
//                request.getAssignedEmployeeId()
//        );
//
//        SupportTicketSource source = actor.getRole() == EmployeeRole.CRM_AGENT
//                ? SupportTicketSource.CRM_CREATED
//                : SupportTicketSource.ADMIN_CREATED;
//
//        SupportTicket ticket = new SupportTicket();
//        ticket.setCustomerUser(customer);
//        ticket.setCategory(request.getCategory());
//        ticket.setPriority(request.getPriority());
//        ticket.setSubject(request.getSubject());
//        ticket.setStatus(SupportTicketStatus.OPEN);
//        ticket.setSource(source);
//        ticket.setCreatedByEmployee(actor);
//        ticket.setAssignedEmployee(assignee);
//        ticket.setLastMessage(preview(request.getMessage()));
//        ticket.setLastRepliedAt(Instant.now());
//
//        SupportTicket savedTicket = ticketRepository.save(ticket);
//
//        SupportTicketReply publicReply = new SupportTicketReply();
//        publicReply.setTicket(savedTicket);
//        publicReply.setSenderType(SupportReplySenderType.EMPLOYEE);
//        publicReply.setSenderEmployee(actor);
//        publicReply.setMessage(request.getMessage());
//        publicReply.setInternalNote(false);
//        replyRepository.save(publicReply);
//
//        if (request.getInternalNote() != null && !request.getInternalNote().isBlank()) {
//            SupportTicketReply internalNote = new SupportTicketReply();
//            internalNote.setTicket(savedTicket);
//            internalNote.setSenderType(SupportReplySenderType.EMPLOYEE);
//            internalNote.setSenderEmployee(actor);
//            internalNote.setMessage(request.getInternalNote());
//            internalNote.setInternalNote(true);
//            replyRepository.save(internalNote);
//        }
//
//        auditLogService.record(
//                AuditAction.SUPPORT_TICKET_CREATED,
//                AuditEntityType.SUPPORT_TICKET,
//                savedTicket.getId(),
//                "Employee created support ticket on behalf of customer"
//        );
//
//        return getTicketDetailForAdmin(savedTicket.getId());
//    }



    @Transactional
    public SupportTicketDetailResponse createTicketFromAdmin(AdminCreateSupportTicketRequest request) {
        EmployeeAccount actor = getCurrentEmployee();

        UserAccount customer = userAccountRepository.findById(request.getCustomerUserId())
                .orElseThrow(() -> new EntityNotFoundException("Customer account not found"));

        EmployeeAccount assignee = resolveAssignedEmployee(
                customer.getId(),
                request.getAssignedEmployeeId()
        );

        SupportTicketSource source = actor.getRole() == EmployeeRole.CRM_AGENT
                ? SupportTicketSource.CRM_CREATED
                : SupportTicketSource.ADMIN_CREATED;

        SupportTicket ticket = new SupportTicket();
        ticket.setCustomerUser(customer);
        ticket.setAssignedEmployee(assignee);
        ticket.setCreatedByEmployee(actor);
        ticket.setSource(source);
        ticket.setSubject(request.getSubject());
        ticket.setCategory(request.getCategory());
        ticket.setPriority(request.getPriority());
        ticket.setStatus(SupportTicketStatus.OPEN);
        ticket.setLastMessage(preview(request.getMessage()));
        ticket.setLastRepliedAt(Instant.now());

        SupportTicket savedTicket = ticketRepository.save(ticket);

        SupportTicketReply firstReply = new SupportTicketReply();
        firstReply.setTicket(savedTicket);
        firstReply.setSenderType(SupportReplySenderType.EMPLOYEE);
        firstReply.setSenderEmployee(actor);
        firstReply.setMessage(request.getMessage());
        firstReply.setInternalNote(false);
        replyRepository.save(firstReply);

        if (request.getInternalNote() != null && !request.getInternalNote().isBlank()) {
            SupportTicketReply internalReply = new SupportTicketReply();
            internalReply.setTicket(savedTicket);
            internalReply.setSenderType(SupportReplySenderType.EMPLOYEE);
            internalReply.setSenderEmployee(actor);
            internalReply.setMessage(request.getInternalNote());
            internalReply.setInternalNote(true);
            replyRepository.save(internalReply);
        }

        auditLogService.record(
                AuditAction.SUPPORT_TICKET_CREATED,
                AuditEntityType.SUPPORT_TICKET,
                savedTicket.getId(),
                "Support ticket created by employee"
        );

        return getTicketDetailForAdmin(savedTicket.getId());
    }

    @Transactional(readOnly = true)
    public SupportTicketPageResponse getAllTickets(
            int page,
            int size,
            SupportTicketStatus status,
            SupportTicketPriority priority,
            SupportTicketCategory category,
            UUID assignedEmployeeId,
            SupportTicketSource source,
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

        Specification<SupportTicket> specification = buildAdminTicketSpecification(
                actor,
                status,
                priority,
                category,
                assignedEmployeeId,
                source,
                search,
                fromDate,
                toDate
        );

        Page<SupportTicket> result = ticketRepository.findAll(specification, pageable);

        return toPageResponse(result);
    }



    private Specification<SupportTicket> buildTicketSpecification(
            EmployeeAccount actor,
            SupportTicketStatus status,
            SupportTicketPriority priority,
            SupportTicketCategory category,
            UUID assignedEmployeeId,
            SupportTicketSource source,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin(actor)) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), actor.getId())
                );
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (assignedEmployeeId != null) {
                predicates.add(
                        cb.equal(root.get("assignedEmployee").get("id"), assignedEmployeeId)
                );
            }

            if (source != null) {
                predicates.add(cb.equal(root.get("source"), source));
            }

            if (fromDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("createdAt"),
                                fromDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant()
                        )
                );
            }

            if (toDate != null) {
                predicates.add(
                        cb.lessThan(
                                root.get("createdAt"),
                                toDate.plusDays(1).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant()
                        )
                );
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("subject")), pattern),
                                cb.like(cb.lower(root.get("lastMessage")), pattern),
                                cb.like(cb.lower(root.get("customerUser").get("phone")), pattern)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }






    @Transactional(readOnly = true)
    public AdminSupportSummaryResponse getAdminSummary() {

        return AdminSupportSummaryResponse.builder()
                .totalTickets(ticketRepository.count())
                .openTickets(ticketRepository.countByStatus(
                        SupportTicketStatus.OPEN))
                .inProgressTickets(ticketRepository.countByStatus(
                        SupportTicketStatus.IN_PROGRESS))
                .waitingCustomerTickets(ticketRepository.countByStatus(
                        SupportTicketStatus.WAITING_CUSTOMER))
                .resolvedTickets(ticketRepository.countByStatus(
                        SupportTicketStatus.RESOLVED))
                .closedTickets(ticketRepository.countByStatus(
                        SupportTicketStatus.CLOSED))
                .build();
    }

    @Transactional(readOnly = true)
    public List<SupportAssigneeResponse> getAssignees() {
        return employeeAccountRepository
                .findByRoleInAndStatusAndDeletedAtIsNullOrderByFullNameAsc(
                        List.of(
                                EmployeeRole.SUPER_ADMIN,
                                EmployeeRole.ADMIN,
                                EmployeeRole.CRM_AGENT
                        ),
                        EmployeeStatus.ACTIVE
                )
                .stream()
                .map(employee ->
                        SupportAssigneeResponse.builder()
                                .employeeId(employee.getId())
                                .fullName(employee.getFullName())
                                .email(employee.getEmail())
                                .role(employee.getRole().name())
                                .assignedDistrict(employee.getAssignedDistrict())
                                .active(true)
                                .build()
                )
                .toList();
    }

    private String resolveCustomerName(
            SupportTicket ticket
    ) {

        UserProfile profile =
                userProfileRepository
                        .findByUserAccountId(
                                ticket.getCustomerUser().getId()
                        )
                        .orElse(null);

        if (profile != null &&
                profile.getCandidateFirstName() != null) {

            return profile.getCandidateFirstName();
        }

        ParentProfile parent =
                parentProfileRepository
                        .findByUserAccountId(
                                ticket.getCustomerUser().getId()
                        )
                        .orElse(null);

        if (parent != null &&
                parent.getParentName() != null) {

            return parent.getParentName();
        }

        return ticket.getCustomerUser().getPhone();
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

    private void assertCanAccessTicket(EmployeeAccount employee, SupportTicket ticket) {
        if (isAdmin(employee)) {
            return;
        }

        if (ticket.getAssignedEmployee() == null
                || !ticket.getAssignedEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You can access only assigned support tickets");
        }
    }

    private EmployeeAccount validateAssignableEmployee(UUID employeeId) {
        EmployeeAccount employee = employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Assigned employee is not active");
        }

        if (!List.of(
                EmployeeRole.SUPER_ADMIN,
                EmployeeRole.ADMIN,
                EmployeeRole.CRM_AGENT
        ).contains(employee.getRole())) {
            throw new IllegalArgumentException("Employee cannot be assigned support tickets");
        }

        return employee;
    }

    private Specification<SupportTicket> buildAdminTicketSpecification(
            EmployeeAccount actor,
            SupportTicketStatus status,
            SupportTicketPriority priority,
            SupportTicketCategory category,
            UUID assignedEmployeeId,
            SupportTicketSource source,
            String search,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            /*
             * Role visibility:
             * SUPER_ADMIN / ADMIN can see all tickets.
             * CRM_AGENT / SUPPORT_AGENT can see only assigned tickets.
             */
            if (!isAdmin(actor)) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("assignedEmployee").get("id"),
                                actor.getId()
                        )
                );
            }

            if (status != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("status"), status)
                );
            }

            if (priority != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("priority"), priority)
                );
            }

            if (category != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("category"), category)
                );
            }

            if (source != null) {
                predicates.add(
                        criteriaBuilder.equal(root.get("source"), source)
                );
            }

            if (assignedEmployeeId != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("assignedEmployee").get("id"),
                                assignedEmployeeId
                        )
                );
            }

            if (fromDate != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
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
                        criteriaBuilder.lessThan(
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
                        criteriaBuilder.or(
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("subject")),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("lastMessage")),
                                        pattern
                                ),
                                criteriaBuilder.like(
                                        criteriaBuilder.lower(root.get("customerUser").get("phone")),
                                        pattern
                                )
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }


}