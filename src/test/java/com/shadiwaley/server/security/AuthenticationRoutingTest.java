package com.shadiwaley.server.security;

import com.shadiwaley.server.auth.api.AuthController;
import com.shadiwaley.server.auth.application.AuthService;
import com.shadiwaley.server.auth.application.RefreshTokenService;
import com.shadiwaley.server.auth.infrastructure.entity.UserRefreshToken;
import com.shadiwaley.server.auth.infrastructure.repository.OtpSessionRepository;
import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotPreferenceRepository;
import com.shadiwaley.server.common.exception.GlobalExceptionHandler;
import com.shadiwaley.server.employee.api.controller.AdminAuthController;
import com.shadiwaley.server.employee.application.service.AdminAuthService;
import com.shadiwaley.server.employee.domain.EmployeeRole;
import com.shadiwaley.server.employee.domain.EmployeeStatus;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(properties = "app.jwt.secret=routing-test-secret-at-least-64-bytes-long-and-only-for-regression-tests")
@ContextConfiguration(classes = {
        AuthController.class, AdminAuthController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, JwtService.class, PasswordConfig.class,
        AuthService.class, RefreshTokenService.class, GlobalExceptionHandler.class
})
class AuthenticationRoutingTest {
    @Autowired MockMvc mvc;
    @Autowired JwtAuthenticationFilter filter;
    @Autowired BCryptPasswordEncoder encoder;
    @SpyBean JwtService jwtService;
    @SpyBean AuthService authService;
    @MockBean AdminAuthService adminAuthService;
    @MockBean UserAccountRepository users;
    @MockBean EmployeeAccountRepository employees;
    @MockBean UserRefreshTokenRepository refreshTokens;
    @MockBean OtpSessionRepository otpSessions;
    @MockBean UserProfileRepository profiles;
    @MockBean ParentProfileRepository parents;
    @MockBean UserPreferencesRepository preferences;
    @MockBean AutopilotPreferenceRepository autopilot;
    @MockBean RestAuthenticationEntryPoint entryPoint;
    @MockBean RestAccessDeniedHandler deniedHandler;

    private UserAccount customer;
    private EmployeeAccount employee;

    @BeforeEach
    void setUp() throws Exception {
        // Isolate routing from the existing private ObjectMapper error-serialization bug.
        doAnswer(call -> {
            call.getArgument(1, HttpServletResponse.class).setStatus(401);
            return null;
        }).when(entryPoint).commence(any(), any(), any());
        doAnswer(call -> {
            call.getArgument(1, HttpServletResponse.class).setStatus(403);
            return null;
        }).when(deniedHandler).handle(any(), any(), any());

        customer = new UserAccount();
        customer.setId(UUID.randomUUID());
        customer.setPhone("9876543210");
        customer.setSide(UserSide.BOY);
        customer.setRole(UserRole.USER);
        customer.setAccountStatus("ACTIVE");
        when(users.findById(customer.getId())).thenReturn(Optional.of(customer));
        employee = new EmployeeAccount();
        employee.setId(UUID.randomUUID());
        employee.setEmail("employee@example.com");
        employee.setRole(EmployeeRole.CRM_AGENT);
        employee.setStatus(EmployeeStatus.ACTIVE);
        when(employees.findById(employee.getId())).thenReturn(Optional.of(employee));
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void refreshCredentialWorksWithoutAccessToken(String operation) throws Exception {
        UserRefreshToken token = validRefreshToken();
        perform(post("/api/v1/auth/" + operation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token.getId() + ".secret\"}"))
                .andExpect(status().isOk());
        verify(refreshTokens).findById(token.getId());
        assertThat(token.getRevokedAt()).isNotNull();
        if (operation.equals("refresh")) {
            assertThat(token.getReplacedByTokenId()).isNotNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void refreshCredentialWorksDespiteInvalidAccessToken(String operation) throws Exception {
        UserRefreshToken token = validRefreshToken();
        perform(post("/api/v1/auth/" + operation)
                        .header("Authorization", "Bearer invalid-access-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token.getId() + ".secret\"}"))
                .andExpect(status().isOk());
        verify(jwtService, never()).isTokenValid(anyString());
        verify(refreshTokens).findById(token.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void invalidRefreshCredentialIsRejectedByService(String operation) throws Exception {
        UserRefreshToken token = validRefreshToken();
        perform(post("/api/v1/auth/" + operation)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token.getId() + ".wrong-secret\"}"))
                .andExpect(status().isBadRequest());
        verify(refreshTokens).findById(token.getId());
        assertThat(token.getRevokedAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST /api/v1/auth/logout-all", "GET /api/v1/admin/auth/me",
            "PATCH /api/v1/admin/auth/change-password"})
    void protectedEndpointsRejectMissingAccessToken(String route) throws Exception {
        String[] parts = route.split(" ");
        perform(request(HttpMethod.valueOf(parts[0]), parts[1]))
                .andExpect(status().isUnauthorized());
        verify(authService, never()).logoutAllDevices();
        verifyNoInteractions(adminAuthService);
    }

    @Test
    void logoutAllUsesCustomerIdentityFromAccessJwt() throws Exception {
        perform(post("/api/v1/auth/logout-all")
                        .header("Authorization", "Bearer " + jwtService.generateCustomerAccessToken(customer)))
                .andExpect(status().isOk());
        verify(authService).logoutAllDevices();
        verify(refreshTokens).revokeAllActiveTokensForUser(eq(customer.getId()), any(Instant.class));
    }

    @Test
    void employeeMeUsesEmployeeIdentityFromAccessJwt() throws Exception {
        doAnswer(call -> {
            assertThat(AuthUser.getCurrentActorId()).isEqualTo(employee.getId());
            assertThat(AuthUser.getCurrentActorType()).isEqualTo(ActorType.EMPLOYEE);
            return null;
        }).when(adminAuthService).me();
        perform(get("/api/v1/admin/auth/me")
                        .header("Authorization", "Bearer " + jwtService.generateEmployeeAccessToken(employee)))
                .andExpect(status().isOk());
        verify(adminAuthService).me();
    }

    @Test
    void changePasswordUsesEmployeeIdentityFromAccessJwt() throws Exception {
        doAnswer(call -> {
            assertThat(AuthUser.getCurrentActorId()).isEqualTo(employee.getId());
            assertThat(AuthUser.getCurrentActorType()).isEqualTo(ActorType.EMPLOYEE);
            return null;
        }).when(adminAuthService).changePassword(any());
        perform(patch("/api/v1/admin/auth/change-password")
                        .header("Authorization", "Bearer " + jwtService.generateEmployeeAccessToken(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1\",\"newPassword\":\"NewPassword2\"}"))
                .andExpect(status().isOk());
        verify(adminAuthService).changePassword(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET /api/v1/admin/auth/me", "PATCH /api/v1/admin/auth/change-password"})
    void customerCannotAccessEmployeeEndpoints(String route) throws Exception {
        String[] parts = route.split(" ");
        perform(request(HttpMethod.valueOf(parts[0]), parts[1])
                        .header("Authorization", "Bearer " + jwtService.generateCustomerAccessToken(customer)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(adminAuthService);
    }

    @Test
    void existingPublicOtpAndLoginRoutesRemainAccessible() throws Exception {
        doReturn(null).when(authService).initiateOtp(any());
        doReturn(null).when(authService).verifyOtp(any());
        doReturn(null).when(authService).resendOtp(any());
        perform(post("/api/v1/auth/otp/initiate").contentType(MediaType.APPLICATION_JSON)
                .content("{\"phone\":\"9876543210\"}")).andExpect(status().isOk());
        perform(post("/api/v1/auth/otp/verify").contentType(MediaType.APPLICATION_JSON)
                .content("{\"tempToken\":\"" + UUID.randomUUID() + "\",\"phone\":\"9876543210\",\"otp\":\"123456\"}"))
                .andExpect(status().isOk());
        perform(post("/api/v1/auth/otp/resend/" + UUID.randomUUID())).andExpect(status().isOk());
        perform(post("/api/v1/admin/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"employee@example.com\",\"password\":\"Password1\"}"))
                .andExpect(status().isOk());
        verify(authService).initiateOtp(any());
        verify(authService).verifyOtp(any());
        verify(authService).resendOtp(any());
        verify(adminAuthService).login(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void skipIsLimitedToExactPostEndpoint(String operation) {
        MockHttpServletRequest postRequest = new MockHttpServletRequest();
        postRequest.setMethod("POST");
        postRequest.setServletPath("/api/v1/auth/" + operation);
        assertThat(filter.shouldNotFilter(postRequest)).isTrue();
        for (String method : new String[]{"GET", "PATCH"}) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setMethod(method);
            request.setServletPath("/api/v1/auth/" + operation);
            assertThat(filter.shouldNotFilter(request)).isFalse();
        }
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setServletPath("/api/v1/auth/" + operation + "/extra");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    private ResultActions perform(MockHttpServletRequestBuilder builder) throws Exception {
        // Match the servlet path supplied by the application's root DispatcherServlet mapping.
        return mvc.perform(builder.with(request -> {
            request.setServletPath(request.getRequestURI());
            return request;
        }));
    }

    private UserRefreshToken validRefreshToken() {
        UserRefreshToken token = new UserRefreshToken();
        token.setId(UUID.randomUUID());
        token.setUserAccount(customer);
        token.setTokenHash(encoder.encode("secret"));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        when(refreshTokens.findById(token.getId())).thenReturn(Optional.of(token));
        return token;
    }
}
