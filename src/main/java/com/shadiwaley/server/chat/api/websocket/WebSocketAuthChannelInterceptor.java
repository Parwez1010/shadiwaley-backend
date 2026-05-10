package com.shadiwaley.server.chat.api.websocket;

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

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new IllegalArgumentException("WebSocket authentication token is required");
            }

            String token = authHeader.substring(7);

            jwtService.isTokenValid(token);

            if (jwtService.extractActorType(token) != ActorType.CUSTOMER) {
                throw new IllegalArgumentException("Only customer accounts can connect to family chat");
            }

            UUID userId = jwtService.extractUserId(token);

            var user = userAccountRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User account not found"));

            var authentication = new UsernamePasswordAuthenticationToken(
                    user.getId(),
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );

            accessor.setUser(authentication);
        }

        return message;
    }
}