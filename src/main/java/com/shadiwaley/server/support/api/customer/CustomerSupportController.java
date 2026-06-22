package com.shadiwaley.server.support.api.customer;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.support.application.SupportTicketService;
import com.shadiwaley.server.support.domain.SupportTicketStatus;
import com.shadiwaley.server.support.dto.request.CreateSupportTicketRequest;
import com.shadiwaley.server.support.dto.request.SupportTicketReplyRequest;
import com.shadiwaley.server.support.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer/support")
@RequiredArgsConstructor
public class CustomerSupportController {

    private final SupportTicketService supportTicketService;

    @PostMapping("/tickets")
    public ApiResponse<SupportTicketDetailResponse> createTicket(
            @Valid @RequestBody CreateSupportTicketRequest request
    ) {
        return ResponseFactory.success(
                "Support ticket created successfully",
                supportTicketService.createTicket(request)
        );
    }

    @GetMapping("/tickets")
    public ApiResponse<SupportTicketPageResponse> getMyTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) SupportTicketStatus status
    ) {
        return ResponseFactory.success(
                "Support tickets fetched successfully",
                supportTicketService.getMyTickets(page, size, status)
        );
    }

    @GetMapping("/tickets/{ticketId}")
    public ApiResponse<SupportTicketDetailResponse> getTicketDetail(
            @PathVariable UUID ticketId
    ) {
        return ResponseFactory.success(
                "Support ticket detail fetched successfully",
                supportTicketService.getMyTicketDetail(ticketId)
        );
    }

    @PostMapping("/tickets/{ticketId}/reply")
    public ApiResponse<SupportTicketDetailResponse> reply(
            @PathVariable UUID ticketId,
            @Valid @RequestBody SupportTicketReplyRequest request
    ) {
        return ResponseFactory.success(
                "Reply added successfully",
                supportTicketService.replyToMyTicket(ticketId, request)
        );
    }

    @PatchMapping("/tickets/{ticketId}/close")
    public ApiResponse<SupportTicketDetailResponse> close(
            @PathVariable UUID ticketId
    ) {
        return ResponseFactory.success(
                "Support ticket closed successfully",
                supportTicketService.closeMyTicket(ticketId)
        );
    }

    @GetMapping("/options")
    public ApiResponse<SupportOptionsResponse> getOptions() {
        return ResponseFactory.success(
                "Support options fetched successfully",
                supportTicketService.getOptions()
        );
    }

    @GetMapping("/summary")
    public ApiResponse<SupportTicketSummaryResponse> getSummary() {
        return ResponseFactory.success(
                "Support summary fetched successfully",
                supportTicketService.getMySummary()
        );
    }
}