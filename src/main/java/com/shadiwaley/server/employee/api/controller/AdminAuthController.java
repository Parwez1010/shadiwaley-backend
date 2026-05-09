package com.shadiwaley.server.employee.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.employee.application.service.AdminAuthService;
import com.shadiwaley.server.employee.dto.request.AdminLoginRequest;
import com.shadiwaley.server.employee.dto.request.ChangePasswordRequest;
import com.shadiwaley.server.employee.dto.response.AdminLoginResponse;
import com.shadiwaley.server.employee.dto.response.AdminMeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseFactory.success(
                "Login successful",
                adminAuthService.login(request)
        );
    }

    @GetMapping("/me")
    public ApiResponse<AdminMeResponse> me() {
        return ResponseFactory.success(
                "Employee profile fetched successfully",
                adminAuthService.me()
        );
    }

    @PatchMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        adminAuthService.changePassword(request);

        return ResponseFactory.success(
                "Password changed successfully",
                null
        );
    }
}