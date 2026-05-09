package com.shadiwaley.server.security;

import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends org.springframework.web.filter.OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserAccountRepository userAccountRepository;
    private final EmployeeAccountRepository employeeAccountRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/admin/auth/")
                || path.equals("/actuator/health");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        try {
            jwtService.isTokenValid(token);

            UUID actorId = jwtService.extractUserId(token);
            ActorType actorType = jwtService.extractActorType(token);

            if (actorType == ActorType.CUSTOMER) {
                authenticateCustomer(actorId);
            } else if (actorType == ActorType.EMPLOYEE) {
                authenticateEmployee(actorId);
            }

        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateCustomer(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId).orElse(null);

        if (user == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

        authentication.setDetails(ActorType.CUSTOMER);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void authenticateEmployee(UUID employeeId) {
        EmployeeAccount employee = employeeAccountRepository.findById(employeeId).orElse(null);

        if (employee == null
                || employee.getStatus() != EmployeeStatus.ACTIVE
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        employee.getId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()))
                );

        authentication.setDetails(ActorType.EMPLOYEE);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}