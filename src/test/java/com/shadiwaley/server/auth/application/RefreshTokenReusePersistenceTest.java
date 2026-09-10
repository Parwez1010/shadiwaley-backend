package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.dto.request.LogoutRequest;
import com.shadiwaley.server.auth.dto.request.RefreshTokenRequest;
import com.shadiwaley.server.auth.infrastructure.entity.UserRefreshToken;
import com.shadiwaley.server.auth.infrastructure.repository.OtpSessionRepository;
import com.shadiwaley.server.auth.infrastructure.repository.UserRefreshTokenRepository;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotPreferenceRepository;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.JwtService;
import com.shadiwaley.server.security.PasswordConfig;
import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.domain.UserSide;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ContextConfiguration(classes = RefreshTokenReusePersistenceTest.PersistenceConfig.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RefreshTokenReusePersistenceTest {
    @TestConfiguration
    @EnableJpaRepositories(basePackageClasses = {UserRefreshTokenRepository.class, UserAccountRepository.class},
            excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = OtpSessionRepository.class))
    @Import({AuthService.class, RefreshTokenService.class, RefreshTokenReuseRevocationService.class, PasswordConfig.class})
    static class PersistenceConfig {
        @Bean
        PersistenceManagedTypes persistenceManagedTypes() {
            return PersistenceManagedTypes.of(UserAccount.class.getName(), UserRefreshToken.class.getName());
        }
    }

    @Autowired AuthService authService;
    @Autowired UserRefreshTokenRepository tokens;
    @Autowired UserAccountRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @MockBean OtpSessionRepository otpSessions;
    @MockBean OtpFailureService otpFailureService;
    @MockBean UserProfileRepository profiles;
    @MockBean ParentProfileRepository parents;
    @MockBean UserPreferencesRepository preferences;
    @MockBean AutopilotPreferenceRepository autopilot;
    @MockBean JwtService jwtService;

    private UserRefreshToken revoked;
    private UserRefreshToken active;
    private UserRefreshToken sibling;
    private UserRefreshToken otherUserToken;

    @BeforeEach
    void seedCommittedTokens() {
        tokens.deleteAll();
        users.deleteAll();
        UserAccount user = createUser("9876543210");
        revoked = createToken(user, Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS));
        active = createToken(user, null);
        sibling = createToken(user, null);
        otherUserToken = createToken(createUser("9876543211"), null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void reuseRevokesAllUsersActiveTokensDespiteException(String operation) {
        assertThatThrownBy(() -> invoke(operation, credential(revoked)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token reuse detected. All sessions revoked.");
        // Fresh repository reads after the outer transaction has rolled back.
        assertThat(read(active).getRevokedAt()).isNotNull();
        assertThat(read(sibling).getRevokedAt()).isNotNull();
        assertThat(read(revoked).getRevokedAt()).isEqualTo(revoked.getRevokedAt());
        assertThat(read(otherUserToken).getRevokedAt()).isNull();
        assertThat(tokens.count()).isEqualTo(4);
        verifyNoInteractions(jwtService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void revokedTokenIdWithoutCorrectSecretCannotRevokeSessions(String operation) {
        assertThatThrownBy(() -> invoke(operation, revoked.getId() + ".wrong-secret"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid refresh token");
        assertThat(read(active).getRevokedAt()).isNull();
        assertThat(read(sibling).getRevokedAt()).isNull();
        assertThat(read(otherUserToken).getRevokedAt()).isNull();
    }

    @Test
    void normalRefreshStillRotatesOnlySuppliedToken() {
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        var result = authService.refresh(refreshRequest(credential(active)));
        assertThat(result.getAccessToken()).isEqualTo("access-token");
        UUID replacementId = UUID.fromString(result.getRefreshToken().split("\\.")[0]);
        assertThat(read(active).getRevokedAt()).isNotNull();
        assertThat(read(active).getReplacedByTokenId()).isEqualTo(replacementId);
        UserRefreshToken replacement = tokens.findById(replacementId).orElseThrow();
        assertThat(replacement.isActive()).isTrue();
        assertThat(encoder.matches(result.getRefreshToken().split("\\.")[1], replacement.getTokenHash())).isTrue();
        assertThat(read(sibling).getRevokedAt()).isNull();
        assertThat(read(otherUserToken).getRevokedAt()).isNull();
    }

    @Test
    void normalLogoutStillRevokesOnlySuppliedToken() {
        invoke("logout", credential(active));
        assertThat(read(active).getRevokedAt()).isNotNull();
        assertThat(read(sibling).getRevokedAt()).isNull();
        assertThat(read(otherUserToken).getRevokedAt()).isNull();
    }

    @Test
    void invalidActiveTokenSecretDoesNotRevokeAnything() {
        assertThatThrownBy(() -> invoke("refresh", active.getId() + ".wrong-secret"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid refresh token");
        assertThat(read(active).getRevokedAt()).isNull();
        assertThat(read(sibling).getRevokedAt()).isNull();
    }

    @Test
    void expiredActiveTokenDoesNotTriggerReuseRevocation() {
        active.setExpiresAt(Instant.now().minusSeconds(60));
        tokens.saveAndFlush(active);
        assertThatThrownBy(() -> invoke("refresh", credential(active)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Refresh token expired");
        assertThat(read(active).getRevokedAt()).isNull();
        assertThat(read(sibling).getRevokedAt()).isNull();
    }

    private void invoke(String operation, String token) {
        if (operation.equals("refresh")) {
            authService.refresh(refreshRequest(token));
        } else {
            LogoutRequest request = new LogoutRequest();
            request.setRefreshToken(token);
            authService.logout(request);
        }
    }

    private RefreshTokenRequest refreshRequest(String token) {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(token);
        return request;
    }

    private String credential(UserRefreshToken token) {
        return token.getId() + ".secret";
    }

    private UserRefreshToken read(UserRefreshToken token) {
        return tokens.findById(token.getId()).orElseThrow();
    }

    private UserAccount createUser(String phone) {
        UserAccount user = new UserAccount();
        user.setPhone(phone);
        user.setSide(UserSide.BOY);
        user.setRole(UserRole.USER);
        return users.saveAndFlush(user);
    }

    private UserRefreshToken createToken(UserAccount user, Instant revokedAt) {
        UserRefreshToken token = new UserRefreshToken();
        token.setUserAccount(user);
        token.setTokenHash(encoder.encode("secret"));
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setRevokedAt(revokedAt);
        return tokens.saveAndFlush(token);
    }
}
