package com.shadiwaley.server.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OtpInitiateResponse {
    private UUID tempToken;
    private String mockOtp;
    private long expiresInSeconds;
}