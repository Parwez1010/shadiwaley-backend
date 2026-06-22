package com.shadiwaley.server.support.api.AdminSupportController;

import com.shadiwaley.server.common.response.*;
import com.shadiwaley.server.support.application.SupportTicketService;
import com.shadiwaley.server.support.domain.*;
import com.shadiwaley.server.support.dto.request.*;
import com.shadiwaley.server.support.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/support")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportTicketService supportTicketService;

    @GetMapping("/tickets")
    public ApiResponse<SupportTicketPageResponse> getTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) SupportTicketStatus status,
            @RequestParam(required = false) SupportTicketPriority priority,
            @RequestParam(required = false) SupportTicketCategory category,
            @RequestParam(required = false) SupportTicketSource source,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ResponseFactory.success(
                "Support tickets fetched successfully",
                supportTicketService.getAllTickets(
                        page,
                        size,
                        status,
                        priority,
                        category,
                        assignedEmployeeId,
                        source,
                        search,
                        fromDate,
                        toDate
                )
        );
    }

    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<SupportTicketDetailResponse> getDetail(
            @PathVariable UUID ticketId
    ) {

        return ResponseFactory.success(
                "Support ticket fetched successfully",
                supportTicketService.getTicketDetailForAdmin(ticketId)
        );
    }

    @PatchMapping("/tickets/{ticketId}/assign")
    public ApiResponse<SupportTicketDetailResponse> assign(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AssignSupportTicketRequest request
    ) {

        return ResponseFactory.success(
                "Support ticket assigned successfully",
                supportTicketService.assignTicket(
                        ticketId,
                        request.getEmployeeId()
                )
        );
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public ApiResponse<SupportTicketDetailResponse> updateStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateSupportTicketStatusRequest request
    ) {

        return ResponseFactory.success(
                "Status updated successfully",
                supportTicketService.updateStatus(
                        ticketId,
                        request.getStatus()
                )
        );
    }

    @PatchMapping("/tickets/{ticketId}/priority")
    public ApiResponse<SupportTicketDetailResponse> updatePriority(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateSupportTicketPriorityRequest request
    ) {

        return ResponseFactory.success(
                "Priority updated successfully",
                supportTicketService.updatePriority(
                        ticketId,
                        request.getPriority()
                )
        );
    }

    @PostMapping("/tickets/{ticketId}/reply")
    public ApiResponse<SupportTicketDetailResponse> reply(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportTicketReplyRequest request
    ) {

        return ResponseFactory.success(
                "Reply added successfully",
                supportTicketService.adminReply(
                        ticketId,
                        request
                )
        );
    }

    @GetMapping("/summary")
    public ApiResponse<AdminSupportSummaryResponse> summary() {

        return ResponseFactory.success(
                "Support summary fetched successfully",
                supportTicketService.getAdminSummary()
        );
    }

    @GetMapping("/assignees")
    public ApiResponse<List<SupportAssigneeResponse>> getAssignees() {
        return ResponseFactory.success(
                "Support assignees fetched successfully",
                supportTicketService.getAssignees()
        );
    }

    @PostMapping
    @PreAuthorize(
            "hasAnyRole('SUPER_ADMIN','ADMIN','CRM_AGENT')"
    )
    public ApiResponse<SupportTicketDetailResponse> createTicket(
            @Valid @RequestBody AdminCreateSupportTicketRequest request
    ) {
        return ResponseFactory.success(
                "Support ticket created successfully",
                supportTicketService.createTicketFromAdmin(request)
        );
    }
}