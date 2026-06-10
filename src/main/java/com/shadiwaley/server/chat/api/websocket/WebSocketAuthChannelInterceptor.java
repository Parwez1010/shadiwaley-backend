package com.shadiwaley.server.chat.api.websocket;

import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.security.ActorType;
import com.shadiwaley.server.security.JwtService;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeAccountRepository employeeAccountRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket authentication token is required");
        }

        String token = authHeader.substring(7);

        jwtService.isTokenValid(token);

        UUID actorId = jwtService.extractUserId(token);
        ActorType actorType = jwtService.extractActorType(token);

        if (actorType == ActorType.CUSTOMER) {
            authenticateCustomer(accessor, actorId);
            return message;
        }

        if (actorType == ActorType.EMPLOYEE) {
            authenticateEmployee(accessor, actorId);
            return message;
        }

        throw new IllegalArgumentException("Unsupported websocket actor type");
    }

    private void authenticateCustomer(StompHeaderAccessor accessor, UUID userId) {
        var user = userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));

        var authentication = new UsernamePasswordAuthenticationToken(
                user.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        authentication.setDetails(ActorType.CUSTOMER);
        accessor.setUser(authentication);
    }

    private void authenticateEmployee(StompHeaderAccessor accessor, UUID employeeId) {
        var employee = employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee account not found"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new IllegalArgumentException("Employee account is not active");
        }

        var authentication = new UsernamePasswordAuthenticationToken(
                employee.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()))
        );

        authentication.setDetails(ActorType.EMPLOYEE);
        accessor.setUser(authentication);
    }
}