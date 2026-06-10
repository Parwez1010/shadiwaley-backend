package com.shadiwaley.server.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshTokenResponse {

    private String accessToken;

    private String refreshToken;
}