package com.shadiwaley.server.chat.api.controller;

import com.shadiwaley.server.chat.application.service.AdminChatMonitorService;
import com.shadiwaley.server.chat.domain.ChatMode;
import com.shadiwaley.server.chat.domain.ChatRoomStatus;
import com.shadiwaley.server.chat.dto.admin.request.AdminSendChatMessageRequest;
import com.shadiwaley.server.chat.dto.request.AdminAssignChatRoomRequest;
import com.shadiwaley.server.chat.dto.request.AdminChatInternalNoteRequest;
import com.shadiwaley.server.chat.dto.response.*;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/chat-monitor")
@RequiredArgsConstructor
public class AdminChatMonitorController {

    private final AdminChatMonitorService adminChatMonitorService;

    @GetMapping("/rooms")
    public ApiResponse<AdminChatRoomPageResponse> getRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) ChatRoomStatus status,
            @RequestParam(required = false) ChatMode chatMode,
            @RequestParam(required = false) Boolean reported,
            @RequestParam(required = false) Boolean needsAttention,
            @RequestParam(required = false) Boolean blocked,
            @RequestParam(required = false) UUID assignedEmployeeId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ResponseFactory.success(
                "Chat monitor rooms fetched successfully",
                adminChatMonitorService.getRooms(
                        page,
                        size,
                        status,
                        chatMode,
                        reported,
                        needsAttention,
                        blocked,
                        assignedEmployeeId,
                        search,
                        fromDate,
                        toDate
                )
        );
    }

    @GetMapping("/rooms/{roomId}")
    public ApiResponse<AdminChatRoomDetailResponse> getRoomDetail(
            @PathVariable UUID roomId
    ) {
        return ResponseFactory.success(
                "Chat monitor room detail fetched successfully",
                adminChatMonitorService.getRoomDetail(roomId)
        );
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<AdminChatMessagePageResponse> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseFactory.success(
                "Chat monitor messages fetched successfully",
                adminChatMonitorService.getMessages(roomId, before, limit)
        );
    }

    @PostMapping("/rooms/{roomId}/notes")
    public ApiResponse<AdminChatInternalNoteResponse> addInternalNote(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminChatInternalNoteRequest request
    ) {
        return ResponseFactory.success(
                "Internal note added successfully",
                adminChatMonitorService.addInternalNote(roomId, request)
        );
    }

    @GetMapping("/rooms/{roomId}/notes")
    public ApiResponse<List<AdminChatInternalNoteResponse>> getInternalNotes(
            @PathVariable UUID roomId
    ) {
        return ResponseFactory.success(
                "Internal notes fetched successfully",
                adminChatMonitorService.getInternalNotes(roomId)
        );
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminChatDashboardResponse> dashboard(){

        return ResponseFactory.success(
                "Dashboard fetched successfully",
                adminChatMonitorService.getDashboard()
        );
    }

    @PatchMapping("/rooms/{roomId}/assign")
    public ApiResponse<Void> assign(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminAssignChatRoomRequest request){

        adminChatMonitorService.assignRoom(
                roomId,
                request.getEmployeeId()
        );

        return ResponseFactory.success(
                "Room assigned successfully",
                null
        );
    }

    @PatchMapping("/rooms/{roomId}/block")
    public ApiResponse<Void> block(
            @PathVariable UUID roomId){

        adminChatMonitorService.blockRoom(roomId);

        return ResponseFactory.success(
                "Room blocked",
                null
        );
    }

    @PatchMapping("/rooms/{roomId}/unblock")
    public ApiResponse<Void> unblock(
            @PathVariable UUID roomId){

        adminChatMonitorService.unblockRoom(roomId);

        return ResponseFactory.success(
                "Room unblocked",
                null
        );
    }

    @PatchMapping("/rooms/{roomId}/close")
    public ApiResponse<Void> close(
            @PathVariable UUID roomId){

        adminChatMonitorService.closeRoomAdmin(roomId);

        return ResponseFactory.success(
                "Room closed",
                null
        );
    }

    @PatchMapping("/messages/{messageId}/hide")
    public ApiResponse<Void> hide(
            @PathVariable UUID messageId){

        adminChatMonitorService.hideMessage(messageId);

        return ResponseFactory.success(
                "Message hidden",
                null
        );
    }

    @PatchMapping("/messages/{messageId}/restore")
    public ApiResponse<Void> restore(
            @PathVariable UUID messageId){

        adminChatMonitorService.restoreMessage(messageId);

        return ResponseFactory.success(
                "Message restored",
                null
        );
    }

    @PostMapping("/rooms/{roomId}/crm-message")
    public ApiResponse<AdminChatMessageResponse> sendCrmMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody AdminSendChatMessageRequest request
    ) {
        return ResponseFactory.success(
                "CRM message sent successfully",
                adminChatMonitorService.sendCrmMessage(roomId, request)
        );
    }

    @GetMapping("/rooms/{roomId}/messages/{messageId}/media/{mediaId}/view")
    public ResponseEntity<byte[]> viewMessageMedia(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @PathVariable UUID mediaId
    ) {
        return adminChatMonitorService.viewMessageMedia(roomId, messageId, mediaId);
    }

    @GetMapping("/reports")
    public ApiResponse<AdminChatReportPageResponse> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseFactory.success(
                "Chat reports fetched successfully",
                adminChatMonitorService.getReports(page, size)
        );
    }

    @PatchMapping("/reports/{reportId}/resolve")
    public ApiResponse<Void> resolveReport(
            @PathVariable UUID reportId
    ) {
        adminChatMonitorService.resolveReport(reportId);

        return ResponseFactory.success(
                "Chat report resolved successfully",
                null
        );
    }

    @PostMapping(
            value = "/rooms/{roomId}/media",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<AdminChatMediaUploadResponse> uploadMedia(
            @PathVariable UUID roomId,
            @RequestPart("file") MultipartFile file,
            @RequestParam UUID assistedUserId
    ) {

        return ResponseFactory.success(
                "CRM chat media uploaded successfully",
                adminChatMonitorService.uploadMedia(
                        roomId,
                        assistedUserId,
                        file
                )
        );
    }

    @GetMapping("/rooms/{roomId}/messages/{messageId}/media/{mediaId}/download")
    public ResponseEntity<byte[]> downloadMessageMedia(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @PathVariable UUID mediaId
    ) {
        return adminChatMonitorService.downloadMessageMedia(roomId, messageId, mediaId);
    }



}