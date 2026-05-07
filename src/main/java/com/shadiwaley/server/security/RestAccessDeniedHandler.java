package com.shadiwaley.server.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shadiwaley.server.common.response.ApiError;
import com.shadiwaley.server.common.response.ResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        var body = ResponseFactory.failure(
                "You do not have permission to access this resource.",
                List.of(new ApiError(
                        null,
                        "ACCESS_DENIED",
                        "This endpoint is protected or your role is not allowed"
                ))
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}