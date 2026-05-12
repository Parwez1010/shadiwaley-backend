package com.shadiwaley.server.admin.application.service;

import com.shadiwaley.server.admin.dto.request.AssignFamilyCrmRequest;
import com.shadiwaley.server.admin.dto.request.CreateAdminFamilyRequest;
import com.shadiwaley.server.admin.dto.request.DeleteFamilyRequest;
import com.shadiwaley.server.admin.dto.request.UpdateAdminFamilyStatusRequest;
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

        CrmCase crmCase = crmCaseRepository.findTop20ByStatusOrderByUpdatedAtDesc(CrmCaseStatus.OPEN)
                .stream()
                .filter(item -> item.getUserAccount().getId().equals(userId))
                .findFirst()
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
}