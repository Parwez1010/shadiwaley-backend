package com.shadiwaley.server.auth.application;

import com.shadiwaley.server.auth.dto.request.OtpInitiateRequest;
import com.shadiwaley.server.auth.dto.request.OtpVerifyRequest;
import com.shadiwaley.server.auth.dto.response.OtpInitiateResponse;
import com.shadiwaley.server.auth.dto.response.OtpVerifyResponse;
import com.shadiwaley.server.auth.infrastructure.entity.OtpSession;
import com.shadiwaley.server.auth.infrastructure.repository.OtpSessionRepository;
import com.shadiwaley.server.autopilot.infrastructure.entity.AutopilotPreference;
import com.shadiwaley.server.autopilot.infrastructure.repository.AutopilotPreferenceRepository;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.preferences.infrastructure.entity.UserPreferences;
import com.shadiwaley.server.preferences.infrastructure.repository.UserPreferencesRepository;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.security.JwtService;
import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final OtpSessionRepository otpSessionRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final AutopilotPreferenceRepository autopilotPreferenceRepository;
    private final JwtService jwtService;

    @Value("${app.otp.expiry-minutes}")
    private long otpExpiryMinutes;

    @Value("${app.otp.max-attempts}")
    private int maxAttempts;

    @Value("${app.otp.resend-cooldown-seconds}")
    private long resendCooldownSeconds;

    @Value("${app.otp.return-otp-in-response}")
    private boolean returnOtpInResponse;

    public OtpInitiateResponse initiateOtp(OtpInitiateRequest request) {
        validateOtpRequestLimit(request.getPhone());
        validateResendCooldown(request.getPhone());

        String otp = generateOtp();

        OtpSession session = new OtpSession();
        session.setPhone(request.getPhone());
        session.setSide(request.getSide());
        session.setOtpCode(otp);
        session.setVerified(false);
        session.setAttemptCount(0);
        session.setExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60));

        OtpSession saved = otpSessionRepository.save(session);

        return OtpInitiateResponse.builder()
                .tempToken(saved.getTempToken())
                .mockOtp(returnOtpInResponse ? otp : null)
                .expiresInSeconds(otpExpiryMinutes * 60)
                .build();
    }

    public OtpInitiateResponse resendOtp(UUID tempToken) {
        OtpSession oldSession = otpSessionRepository.findByTempToken(tempToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid temp token"));

        if (oldSession.isVerified()) {
            throw new IllegalArgumentException("OTP is already verified");
        }

        validateOtpRequestLimit(oldSession.getPhone());
        validateResendCooldown(oldSession.getPhone());

        String otp = generateOtp();

        OtpSession session = new OtpSession();
        session.setPhone(oldSession.getPhone());
        session.setSide(oldSession.getSide());
        session.setOtpCode(otp);
        session.setVerified(false);
        session.setAttemptCount(0);
        session.setExpiresAt(Instant.now().plusSeconds(otpExpiryMinutes * 60));

        OtpSession saved = otpSessionRepository.save(session);

        return OtpInitiateResponse.builder()
                .tempToken(saved.getTempToken())
                .mockOtp(returnOtpInResponse ? otp : null)
                .expiresInSeconds(otpExpiryMinutes * 60)
                .build();
    }

    @Transactional
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest request) {
        OtpSession session = otpSessionRepository.findByTempToken(request.getTempToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid temp token"));

        if (!session.getPhone().equals(request.getPhone())) {
            throw new IllegalArgumentException("Phone does not match OTP session");
        }

        if (session.isVerified()) {
            throw new IllegalArgumentException("OTP is already verified");
        }

        if (session.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("OTP expired. Please request a new OTP.");
        }

        if (session.getAttemptCount() >= maxAttempts) {
            throw new IllegalArgumentException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        if (!session.getOtpCode().equals(request.getOtp())) {
            session.setAttemptCount(session.getAttemptCount() + 1);
            otpSessionRepository.save(session);

            int remainingAttempts = maxAttempts - session.getAttemptCount();
            throw new IllegalArgumentException("Invalid OTP. Remaining attempts: " + remainingAttempts);
        }

        session.setVerified(true);
        otpSessionRepository.save(session);

        boolean isNewUser = false;

        UserAccount userAccount = userAccountRepository.findByPhone(request.getPhone())
                .orElseGet(() -> {
                    UserAccount account = new UserAccount();
                    account.setPhone(request.getPhone());
                    account.setPhoneVerified(true);
                    account.setSide(session.getSide());
                    account.setRole(UserRole.USER);
                    account.setAccountStatus("ACTIVE");
                    return userAccountRepository.save(account);
                });

        if (userProfileRepository.findByUserAccountId(userAccount.getId()).isEmpty()) {
            isNewUser = true;

            UserProfile profile = new UserProfile();
            profile.setUserAccount(userAccount);
            profile.setDisplayId(generateDisplayId(session.getSide().name()));
            UserProfile savedProfile = userProfileRepository.save(profile);

            ParentProfile parent = new ParentProfile();
            parent.setUserAccount(userAccount);
            parent.setParentPhone(userAccount.getPhone());
            parentProfileRepository.save(parent);

            UserPreferences preferences = new UserPreferences();
            preferences.setUserProfile(savedProfile);
            userPreferencesRepository.save(preferences);

            AutopilotPreference autopilot = new AutopilotPreference();
            autopilot.setUserAccount(userAccount);
            autopilot.setWhatsappNumber(userAccount.getPhone());
            autopilotPreferenceRepository.save(autopilot);
        }

        userAccount.setLastLoginAt(Instant.now());
        userAccountRepository.save(userAccount);

        UserProfile profile = userProfileRepository.findByUserAccountId(userAccount.getId())
                .orElseThrow(() -> new IllegalArgumentException("Profile not found"));

        String accessToken = jwtService.generateAccessToken(userAccount);
        String refreshToken = UUID.randomUUID().toString();

        return OtpVerifyResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userAccount.getId())
                .profileId(profile.getId())
                .side(userAccount.getSide())
                .isNewUser(isNewUser)
                .build();
    }

    private void validateOtpRequestLimit(String phone) {
        Instant oneHourAgo = Instant.now().minusSeconds(3600);
        long requestCount = otpSessionRepository.countByPhoneAndCreatedAtAfter(phone, oneHourAgo);

        if (requestCount >= 3) {
            throw new IllegalArgumentException("OTP request limit exceeded. Please try again after some time.");
        }
    }

    private void validateResendCooldown(String phone) {
        otpSessionRepository.findTopByPhoneOrderByCreatedAtDesc(phone)
                .ifPresent(lastSession -> {
                    Instant allowedAfter = lastSession.getCreatedAt().plusSeconds(resendCooldownSeconds);
                    if (Instant.now().isBefore(allowedAfter)) {
                        long waitSeconds = allowedAfter.getEpochSecond() - Instant.now().getEpochSecond();
                        throw new IllegalArgumentException("Please wait " + waitSeconds + " seconds before requesting another OTP.");
                    }
                });
    }

    private String generateOtp() {
        int otp = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    private String generateDisplayId(String side) {
        String prefix = side.equals("BOY") ? "BL" : "GL";
        String random = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "#" + prefix + "-" + random;
    }
}