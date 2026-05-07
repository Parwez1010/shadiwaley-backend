package com.shadiwaley.server.auth.dto.response;

import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class OtpVerifyResponse {
    private String accessToken;
    private String refreshToken;
    private UUID userId;
    private UUID profileId;
    private UserSide side;
    private boolean isNewUser;
}