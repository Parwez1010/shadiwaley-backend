package com.shadiwaley.server.auth.api;

import com.shadiwaley.server.auth.dto.request.OtpInitiateRequest;
import com.shadiwaley.server.auth.dto.request.OtpVerifyRequest;
import com.shadiwaley.server.auth.dto.response.OtpInitiateResponse;
import com.shadiwaley.server.auth.dto.response.OtpVerifyResponse;
import com.shadiwaley.server.auth.application.AuthService;
import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/otp/initiate")
    public ApiResponse<OtpInitiateResponse> initiateOtp(@Valid @RequestBody OtpInitiateRequest request) {
        return ResponseFactory.success("OTP sent successfully", authService.initiateOtp(request));
    }

    @PostMapping("/otp/resend/{tempToken}")
    public ApiResponse<OtpInitiateResponse> resendOtp(@PathVariable UUID tempToken) {
        return ResponseFactory.success("OTP resent successfully", authService.resendOtp(tempToken));
    }

    @PostMapping("/otp/verify")
    public ApiResponse<OtpVerifyResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest request) {
        return ResponseFactory.success("OTP verified successfully", authService.verifyOtp(request));
    }
}