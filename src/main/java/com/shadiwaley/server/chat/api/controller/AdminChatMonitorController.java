package com.shadiwaley.server.chat.api.controller;

import com.shadiwaley.server.chat.application.service.AdminChatMonitorService;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.dto.admin.request.*;
import com.shadiwaley.server.chat.dto.admin.response.*;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/chat-monitor")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
public class AdminChatMonitorController {

    private final AdminChatMonitorService adminChatMonitorService;

    @GetMapping("/summary")
    public ApiResponse<AdminChatMonitorSummaryResponse> summary(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) UUID assignedEmployeeId
    ) {
        return ResponseFactory.success(
                "Chat monitor summary fetched successfully",
                adminChatMonitorService.getSummary(fromDate, toDate, assignedEmployeeId)
        );
    }

    @GetMapping("/rooms")
    public ApiResponse<AdminChatRoomPageResponse> rooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ChatRoomStatus status,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) UUID crmCaseId,
            @RequestParam(required = false) UUID proposalId,
            @RequestParam(required = false) UUID pipelineId,
            @RequestParam(required = false) UUID fromProfileId,
            @RequestParam(required = false) UUID toProfileId,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String side,
            @RequestParam(required = false) Boolean hasUnread,
            @RequestParam(required = false) Boolean reportedOnly,
            @RequestParam(required = false) Boolean needsAttentionOnly,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            @RequestParam(required = false) String sort
    ) {
        return ResponseFactory.success(
                "Chat rooms fetched successfully",
                adminChatMonitorService.getRooms(
                        page,
                        size,
                        search,
                        status,
                        assignedEmployeeId,
                        crmCaseId,
                        proposalId,
                        pipelineId,
                        fromProfileId,
                        toProfileId,
                        district,
                        side,
                        hasUnread,
                        reportedOnly,
                        needsAttentionOnly,
                        fromDate,
                        toDate,
                        sort
                )
        );
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<AdminChatRoomDetailResponse> roomDetail(
            @PathVariable UUID roomId
    ) {
        return ResponseFactory.success(
                "Chat room detail fetched successfully",
                adminChatMonitorService.getRoomDetail(roomId)
        );
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<AdminChatMessagePageResponse> messages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            @RequestParam(required = false) UUID beforeMessageId,
            @RequestParam(required = false) UUID afterMessageId,
            @RequestParam(required = false) String sort
    ) {
        return ResponseFactory.success(
                "Chat messages fetched successfully",
                adminChatMonitorService.getMessages(
                        roomId,
                        page,
                        size,
                        beforeMessageId,
                        afterMessageId,
                        sort
                )
        );
    }

    @PostMapping("/rooms/{roomId}/notes")
    public ApiResponse<AdminChatNoteResponse> addNote(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatNoteRequest request
    ) {
        return ResponseFactory.success(
                "Chat note added successfully",
                adminChatMonitorService.addNote(roomId, request)
        );
    }

    @PatchMapping("/rooms/{roomId}/status")
    public ApiResponse<AdminChatStatusUpdateResponse> updateStatus(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatStatusUpdateRequest request
    ) {
        return ResponseFactory.success(
                "Chat room status updated successfully",
                adminChatMonitorService.updateStatus(roomId, request)
        );
    }

    @PatchMapping("/messages/{messageId}/moderation")
    public ApiResponse<AdminMessageModerationResponse> moderateMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody AdminMessageModerationRequest request
    ) {
        return ResponseFactory.success(
                "Message moderation updated successfully",
                adminChatMonitorService.moderateMessage(messageId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/report")
    public ApiResponse<AdminChatStatusUpdateResponse> reportRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatActionReasonRequest request
    ) {
        return ResponseFactory.success(
                "Chat room reported successfully",
                adminChatMonitorService.reportRoom(roomId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/block")
    public ApiResponse<AdminChatStatusUpdateResponse> blockRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatActionReasonRequest request
    ) {
        return ResponseFactory.success(
                "Chat room blocked successfully",
                adminChatMonitorService.blockRoom(roomId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/unblock")
    public ApiResponse<AdminChatStatusUpdateResponse> unblockRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatActionReasonRequest request
    ) {
        return ResponseFactory.success(
                "Chat room unblocked successfully",
                adminChatMonitorService.unblockRoom(roomId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/close")
    public ApiResponse<AdminChatStatusUpdateResponse> closeRoom(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatCloseRequest request
    ) {
        return ResponseFactory.success(
                "Chat room closed successfully",
                adminChatMonitorService.closeRoom(roomId, request)
        );
    }

    @PatchMapping("/rooms/{roomId}/assignment")
    public ApiResponse<AdminChatAssignmentResponse> updateAssignment(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatAssignmentRequest request
    ) {
        return ResponseFactory.success(
                "Chat room assignment updated successfully",
                adminChatMonitorService.updateAssignment(roomId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/decision")
    public ApiResponse<AdminChatDecisionResponse> recordDecision(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatDecisionRequest request
    ) {
        return ResponseFactory.success(
                "Family decision recorded successfully",
                adminChatMonitorService.recordDecision(roomId, request)
        );
    }

    @PostMapping("/rooms/{roomId}/follow-up")
    public ApiResponse<AdminChatFollowUpResponse> scheduleFollowUp(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatFollowUpRequest request
    ) {
        return ResponseFactory.success(
                "Chat follow-up scheduled successfully",
                adminChatMonitorService.scheduleFollowUp(roomId, request)
        );
    }

    @GetMapping("/rooms/{roomId}/notes")
    public ApiResponse<AdminChatHistoryPageResponse<AdminChatNoteResponse>> getNotes(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Chat notes fetched successfully",
                adminChatMonitorService.getNotes(roomId, page, size)
        );
    }

    @GetMapping("/rooms/{roomId}/decisions")
    public ApiResponse<AdminChatHistoryPageResponse<AdminChatDecisionLogResponse>> getDecisions(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Chat decisions fetched successfully",
                adminChatMonitorService.getDecisions(roomId, page, size)
        );
    }

    @GetMapping("/rooms/{roomId}/follow-ups")
    public ApiResponse<AdminChatHistoryPageResponse<AdminChatFollowUpResponse>> getFollowUps(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Chat follow-ups fetched successfully",
                adminChatMonitorService.getFollowUps(roomId, page, size)
        );
    }


}