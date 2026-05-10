package com.shadiwaley.server.safety.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.safety.application.service.UserSafetyService;
import com.shadiwaley.server.safety.dto.request.BlockUserRequest;
import com.shadiwaley.server.safety.dto.request.ReportUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/safety/users")
@RequiredArgsConstructor
public class UserSafetyController {

    private final UserSafetyService userSafetyService;

    @PostMapping("/{userId}/block")
    public ApiResponse<Void> blockUser(
            @PathVariable UUID userId,
            @Valid @RequestBody BlockUserRequest request
    ) {
        userSafetyService.blockUser(userId, request);
        return ResponseFactory.success("User blocked successfully", null);
    }

    @DeleteMapping("/{userId}/block")
    public ApiResponse<Void> unblockUser(@PathVariable UUID userId) {
        userSafetyService.unblockUser(userId);
        return ResponseFactory.success("User unblocked successfully", null);
    }

    @PostMapping("/{userId}/report")
    public ApiResponse<Void> reportUser(
            @PathVariable UUID userId,
            @Valid @RequestBody ReportUserRequest request
    ) {
        userSafetyService.reportUser(userId, request);
        return ResponseFactory.success("User reported successfully", null);
    }
}