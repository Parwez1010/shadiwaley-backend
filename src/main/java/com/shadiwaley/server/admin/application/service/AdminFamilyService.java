package com.shadiwaley.server.admin.application.service;

import com.shadiwaley.server.admin.dto.request.*;
import com.shadiwaley.server.admin.dto.response.CrmFamilyDetailResponse;
import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.crm.domain.CrmCasePriority;
import com.shadiwaley.server.crm.domain.CrmCaseStatus;
import com.shadiwaley.server.crm.domain.CrmCaseType;
import com.shadiwaley.server.crm.infrastructure.entity.CrmCase;
import com.shadiwaley.server.crm.infrastructure.repository.CrmCaseRepository;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.onboarding.application.service.OnboardingService;
import com.shadiwaley.server.onboarding.dto.request.OnboardingProfileUpsertRequest;
import com.shadiwaley.server.parent.infrastructure.entity.ParentProfile;
import com.shadiwaley.server.parent.infrastructure.repository.ParentProfileRepository;
import com.shadiwaley.server.profile.domain.ProfileStatus;
import com.shadiwaley.server.profile.infrastructure.entity.UserProfile;
import com.shadiwaley.server.profile.infrastructure.repository.UserProfileRepository;
import com.shadiwaley.server.revenue.application.service.RevenueOnboardingService;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.domain.SubscriptionSource;
import com.shadiwaley.server.revenue.dto.response.SubscriptionResponse;
import com.shadiwaley.server.security.AuthUser;
import com.shadiwaley.server.user.domain.UserRole;
import com.shadiwaley.server.user.infrastructure.entity.UserAccount;
import com.shadiwaley.server.user.infrastructure.repository.UserAccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminFamilyService {

    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final ParentProfileRepository parentProfileRepository;
    private final EmployeeAccountRepository employeeAccountRepository;
    private final CrmCaseRepository crmCaseRepository;
    private final OnboardingService onboardingService;
    private final AdminDashboardService adminDashboardService;
    private final AuditLogService auditLogService;
    private final RevenueOnboardingService revenueOnboardingService;
    private final RevenueService revenueService;



    @Transactional
    public CrmFamilyDetailResponse createFamily(CreateAdminFamilyRequest request) {
        if (userAccountRepository.existsByPhone(request.getPhone())) {
            throw new IllegalArgumentException("A family already exists with this phone number");
        }

        UserAccount account = new UserAccount();
        account.setPhone(request.getPhone());
        account.setSide(request.getSide());
        account.setRole(UserRole.CUSTOMER);
        account.setAccountStatus("ACTIVE");
        account.setPhoneVerified(true);
        account.setCreatedAt(Instant.now());
        account.setUpdatedAt(Instant.now());

        UserAccount savedAccount = userAccountRepository.save(account);

        onboardingService.upsertProfileForUser(savedAccount.getId(), request.getOnboarding());

        UserProfile profile = userProfileRepository.findByUserAccountId(savedAccount.getId())
                .orElseThrow(() -> new EntityNotFoundException("Created profile not found"));

        revenueOnboardingService.applyOnboardingPlan(
                savedAccount.getId(),
                profile.getId(),
                request.getPlan(),
                SubscriptionSource.FAMILY_ONBOARDING
        );

        createDefaultCrmCase(savedAccount, profile);

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_ACCOUNT,
                savedAccount.getId(),
                "Family created by admin/CRM"
        );

        return adminDashboardService.getFamilyDetail(savedAccount.getId());
    }

    @Transactional
    public CrmFamilyDetailResponse updateOnboarding(UUID userId, OnboardingProfileUpsertRequest request) {
        UserAccount account = getUser(userId);

        onboardingService.upsertProfileForUser(account.getId(), request);

        UserProfile profile = userProfileRepository.findByUserAccountId(account.getId())
                .orElseThrow(() -> new EntityNotFoundException("Profile not found"));

        if (request.getPlan() != null) {
            revenueOnboardingService.applyOnboardingPlan(
                    account.getId(),
                    profile.getId(),
                    request.getPlan(),
                    SubscriptionSource.FAMILY_DETAIL
            );
        }

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_ACCOUNT,
                account.getId(),
                "Family onboarding updated by admin/CRM"
        );

        return adminDashboardService.getFamilyDetail(account.getId());
    }

    @Transactional
    public CrmFamilyDetailResponse updateStatus(UUID userId, UpdateAdminFamilyStatusRequest request) {
        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        profile.setProfileStatus(request.getProfileStatus());

        UserProfile saved = userProfileRepository.save(profile);

        String reason = request.getReason() == null || request.getReason().isBlank()
                ? "No reason provided"
                : request.getReason();

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_PROFILE,
                saved.getId(),
                "Family profile status changed to " + request.getProfileStatus(),
                "reason=" + reason
        );

        return adminDashboardService.getFamilyDetail(userId);
    }

    @Transactional
    public CrmFamilyDetailResponse assignCrm(UUID userId, AssignFamilyCrmRequest request) {
        UserAccount account = getUser(userId);

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        EmployeeAccount employee = employeeAccountRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        CrmCase crmCase = crmCaseRepository
                .findTopByUserAccountIdOrderByUpdatedAtDesc(userId)
                .orElseGet(() -> {
                    CrmCase newCase = new CrmCase();
                    newCase.setUserAccount(account);
                    newCase.setUserProfile(profile);
                    newCase.setCaseType(CrmCaseType.PROFILE_ONBOARDING);
                    newCase.setPriority(CrmCasePriority.MEDIUM);
                    newCase.setStatus(CrmCaseStatus.OPEN);
                    newCase.setSource("ADMIN_PANEL");
                    newCase.setSummary("Family assigned from admin panel");
                    return newCase;
                });

        crmCase.setAssignedEmployee(employee);
        crmCase.setStatus(CrmCaseStatus.IN_PROGRESS);

        crmCaseRepository.save(crmCase);

        auditLogService.record(
                AuditAction.CRM_CASE_ASSIGNED,
                AuditEntityType.CRM_CASE,
                crmCase.getId(),
                "Family assigned to CRM employee " + employee.getFullName()
        );

        return adminDashboardService.getFamilyDetail(userId);
    }

    private void createDefaultCrmCase(UserAccount account, UserProfile profile) {
        CrmCase crmCase = new CrmCase();
        crmCase.setUserAccount(account);
        crmCase.setUserProfile(profile);
        crmCase.setCaseType(CrmCaseType.PROFILE_ONBOARDING);
        crmCase.setPriority(CrmCasePriority.MEDIUM);
        crmCase.setStatus(CrmCaseStatus.OPEN);
        crmCase.setSource("ADMIN_PANEL");
        crmCase.setSummary("Family created from admin panel");

        crmCaseRepository.save(crmCase);
    }

    private UserAccount getUser(UUID userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));
    }

    @Transactional
    public void deleteFamily(UUID userId, DeleteFamilyRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        if (profile.getProfileStatus() != ProfileStatus.SUSPENDED) {
            throw new IllegalArgumentException("Only suspended families can be permanently deleted");
        }

        String reason = request.getReason() == null || request.getReason().isBlank()
                ? "No reason provided"
                : request.getReason();

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_ACCOUNT,
                account.getId(),
                "Family permanently deleted by Super Admin",
                "reason=" + reason + ", phone=" + account.getPhone() + ", profileId=" + profile.getId()
        );

        userAccountRepository.delete(account);
    }

    @Transactional
    public CrmFamilyDetailResponse submitForReview(UUID userId, SubmitFamilyReviewRequest request) {
        UserAccount account = userAccountRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User account not found"));

        UserProfile profile = userProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("User profile not found"));

        ParentProfile parent = parentProfileRepository.findByUserAccountId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Parent profile not found"));

        validateBeforeSubmit(profile, parent);

        profile.setProfileStatus(ProfileStatus.PENDING_VERIFICATION);
        userProfileRepository.save(profile);

        String note = request.getNote() == null || request.getNote().isBlank()
                ? "Family onboarding submitted for review"
                : request.getNote();

        auditLogService.record(
                AuditAction.SYSTEM_ACTION,
                AuditEntityType.USER_PROFILE,
                profile.getId(),
                note
        );

        return adminDashboardService.getFamilyDetail(account.getId());
    }

    private void validateBeforeSubmit(UserProfile profile, ParentProfile parent) {
        if (parent.getParentName() == null || parent.getParentName().isBlank()) {
            throw new IllegalArgumentException("Parent name is required before submitting for review");
        }

        if (parent.getDistrict() == null || parent.getDistrict().isBlank()) {
            throw new IllegalArgumentException("District is required before submitting for review");
        }

        if (profile.getCandidateFirstName() == null || profile.getCandidateFirstName().isBlank()) {
            throw new IllegalArgumentException("Candidate name is required before submitting for review");
        }

        if (profile.getCandidateAge() == null) {
            throw new IllegalArgumentException("Candidate age is required before submitting for review");
        }
    }

    private void applySubscriptionFields(
            CrmFamilyDetailResponse.CrmFamilyDetailResponseBuilder builder,
            UUID userId
    ) {
        try {
            var summary = revenueService.getFamilySubscription(userId);
            var subscription = summary.getCurrentSubscription();

            if (subscription == null) {
                return;
            }

            builder.subscriptionId(subscription.getSubscriptionId())
                    .planCode(subscription.getPlanCode())
                    .planName(subscription.getPlanName())
                    .planAmount(subscription.getAmount())
                    .paymentStatus(subscription.getPaymentStatus() != null
                            ? subscription.getPaymentStatus().name()
                            : null)
                    .subscriptionStatus(subscription.getSubscriptionStatus() != null
                            ? subscription.getSubscriptionStatus().name()
                            : null);
        } catch (Exception ignored) {
        }
    }
}