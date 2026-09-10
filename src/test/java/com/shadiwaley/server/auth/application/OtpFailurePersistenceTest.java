package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.dto.request.OtpVerifyRequest;
import com.shadiwaley.server.auth.infrastructure.entity.OtpSession;
import com.shadiwaley.server.auth.infrastructure.repository.OtpSessionRepository;
import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotPreferenceRepository;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.JwtService;
import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.otp.max-attempts=5"})
@ContextConfiguration(classes = OtpFailurePersistenceTest.PersistenceConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OtpFailurePersistenceTest {
    @TestConfiguration
    @EnableJpaRepositories(basePackageClasses = OtpSessionRepository.class,
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                    classes = UserRefreshTokenRepository.class))
    @Import({AuthService.class, OtpFailureService.class})
    static class PersistenceConfig {
        @Bean
        PersistenceManagedTypes persistenceManagedTypes() {
            return PersistenceManagedTypes.of(OtpSession.class.getName());
        }
    }

    @Autowired AuthService authService;
    @Autowired OtpSessionRepository sessions;
    @MockBean UserAccountRepository users;
    @MockBean UserProfileRepository profiles;
    @MockBean ParentProfileRepository parents;
    @MockBean UserPreferencesRepository preferences;
    @MockBean AutopilotPreferenceRepository autopilot;
    @MockBean JwtService jwtService;
    @MockBean RefreshTokenService refreshTokenService;
    private UUID sessionId;
    private UUID tempToken;

    @BeforeEach
    void createSession() {
        sessions.deleteAll();
        OtpSession session = new OtpSession();
        session.setPhone("9876543210");
        session.setSide(UserSide.BOY);
        session.setGuardianName("Existing guardian");
        session.setOtpCode("123456");
        session.setExpiresAt(Instant.now().plusSeconds(300));
        OtpSession saved = sessions.saveAndFlush(session);
        sessionId = saved.getId();
        tempToken = saved.getTempToken();
    }

    @Test
    void firstWrongOtpCommitsCounterDespiteAuthenticationException() {
        rejectWrongOtp(4);
        assertThat(readSession().getAttemptCount()).isEqualTo(1);
        assertThat(readSession().isVerified()).isFalse();
        assertThat(readSession().getGuardianName()).isEqualTo("Existing guardian");
        verifyNoInteractions(users, jwtService, refreshTokenService);
    }

    @Test
    void failuresAccumulateToFiveAndThenEvenCorrectOtpIsRejected() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            rejectWrongOtp(5 - attempt);
            assertThat(readSession().getAttemptCount()).isEqualTo(attempt);
        }
        assertThatThrownBy(() -> authService.verifyOtp(request("123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum OTP attempts exceeded. Please request a new OTP.");
        assertThat(readSession().getAttemptCount()).isEqualTo(5);
        assertThat(readSession().isVerified()).isFalse();
        verifyNoInteractions(users, jwtService, refreshTokenService);
    }

    @Test
    void fifthWrongOtpPersistsThreshold() {
        setAttemptCount(4);
        rejectWrongOtp(0);
        assertThat(readSession().getAttemptCount()).isEqualTo(5);
    }

    @Test
    void persistedThresholdRejectsCorrectOtpWithoutChangingSession() {
        setAttemptCount(5);
        assertThatThrownBy(() -> authService.verifyOtp(request("123456")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Maximum OTP attempts exceeded. Please request a new OTP.");
        assertThat(readSession().getAttemptCount()).isEqualTo(5);
        assertThat(readSession().isVerified()).isFalse();
    }

    @Test
    void correctOtpBelowThresholdCommitsVerificationAndReturnsTokens() {
        setAttemptCount(2);
        UserAccount user = stubExistingCustomer();
        when(jwtService.generateAccessToken(user)).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(eq(user), anyString(), anyString())).thenReturn("refresh-token");
        var response = authService.verifyOtp(request("123456"));
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(user.getId());
        assertThat(readSession().isVerified()).isTrue();
        assertThat(readSession().getAttemptCount()).isEqualTo(2);
        assertThat(readSession().getGuardianName()).isEqualTo("Existing guardian");
        assertThatThrownBy(() -> authService.verifyOtp(request("123456")))
                .hasMessage("OTP is already verified");
    }

    @Test
    void downstreamFailureStillRollsBackSuccessfulVerification() {
        UserAccount user = stubExistingCustomer();
        when(jwtService.generateAccessToken(user)).thenThrow(new IllegalStateException("Token generation failed"));
        assertThatThrownBy(() -> authService.verifyOtp(request("123456")))
                .isInstanceOf(IllegalStateException.class).hasMessage("Token generation failed");
        assertThat(readSession().isVerified()).isFalse();
        assertThat(readSession().getAttemptCount()).isZero();
    }

    private void rejectWrongOtp(int remaining) {
        assertThatThrownBy(() -> authService.verifyOtp(request("654321")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid OTP. Remaining attempts: " + remaining);
    }

    private OtpSession readSession() {
        // No test transaction: this reads committed state after verifyOtp has thrown/returned.
        return sessions.findById(sessionId).orElseThrow();
    }

    private void setAttemptCount(int count) {
        OtpSession session = readSession();
        session.setAttemptCount(count);
        sessions.saveAndFlush(session);
    }

    private OtpVerifyRequest request(String otp) {
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setTempToken(tempToken);
        request.setPhone("9876543210");
        request.setOtp(otp);
        return request;
    }

    private UserAccount stubExistingCustomer() {
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setPhone("9876543210");
        user.setSide(UserSide.BOY);
        user.setRole(UserRole.USER);
        UserProfile profile = new UserProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserAccount(user);
        when(users.findByPhone(user.getPhone())).thenReturn(Optional.of(user));
        when(profiles.findByUserAccountId(user.getId())).thenReturn(Optional.of(profile));
        return user;
    }
}
