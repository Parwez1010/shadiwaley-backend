package com.shadiwaley.server.chat.api.controller;

import com.shadiwaley.server.chat.application.service.FamilyChatService;
import com.shadiwaley.server.chat.dto.request.*;
import com.shadiwaley.server.chat.dto.response.*;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class FamilyChatController {

    private final FamilyChatService familyChatService;

    @GetMapping("/rooms")
    public ApiResponse<List<ChatRoomResponse>> getMyRooms() {
        return ResponseFactory.success(
                "Chat rooms fetched successfully",
                familyChatService.getMyRooms()
        );
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessagePageResponse> getMessages(
            @PathVariable UUID roomId,
            @RequestParam(required = false) Instant before,
            @RequestParam(defaultValue = "50") int limit
    ) {
        return ResponseFactory.success(
                "Messages fetched successfully",
                familyChatService.getMessages(roomId, before, limit)
        );
    }

    @PostMapping("/rooms/{roomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @PathVariable UUID roomId,
            @Valid @RequestBody SendChatMessageRequest request
    ) {
        return ResponseFactory.success(
                "Message sent successfully",
                familyChatService.sendMessage(roomId, request)
        );
    }

    @PatchMapping("/rooms/{roomId}/messages/{messageId}")
    public ApiResponse<ChatMessageResponse> editMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody UpdateChatMessageRequest request
    ) {
        return ResponseFactory.success(
                "Message updated successfully",
                familyChatService.editMessage(roomId, messageId, request)
        );
    }

    @DeleteMapping("/rooms/{roomId}/messages/{messageId}")
    public ApiResponse<Void> deleteMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId
    ) {
        familyChatService.deleteMessage(roomId, messageId);

        return ResponseFactory.success("Message deleted successfully", null);
    }

    @PatchMapping("/rooms/{roomId}/read")
    public ApiResponse<Void> markRoomAsRead(@PathVariable UUID roomId) {
        familyChatService.markRoomAsRead(roomId);

        return ResponseFactory.success("Chat marked as read", null);
    }

    @PostMapping("/rooms/{roomId}/block")
    public ApiResponse<Void> blockRoom(@PathVariable UUID roomId) {

        familyChatService.blockRoom(roomId);

        return ResponseFactory.success(
                "Chat room blocked successfully",
                null
        );
    }

    @PostMapping("/rooms/{roomId}/close")
    public ApiResponse<Void> closeRoom(@PathVariable UUID roomId) {

        familyChatService.closeRoom(roomId);

        return ResponseFactory.success(
                "Chat room closed successfully",
                null
        );
    }

    @PostMapping("/rooms/{roomId}/messages/{messageId}/report")
    public ApiResponse<Void> reportMessage(
            @PathVariable UUID roomId,
            @PathVariable UUID messageId,
            @Valid @RequestBody ReportChatMessageRequest request
    ) {

        familyChatService.reportMessage(roomId, messageId, request);

        return ResponseFactory.success(
                "Message reported successfully",
                null
        );
    }

}