package com.shadiwaley.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadiwaley.server.common.response.ApiError;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = ResponseFactory.failure(
                "Authentication required. Please login and send a valid access token.",
                List.of(new ApiError(
                        "Authorization",
                        "AUTHENTICATION_REQUIRED",
                        "Missing or invalid Bearer token"
                ))
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}