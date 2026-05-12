package com.shadiwaley.server.employee.application.service;

import com.shadiwaley.server.audit.application.service.AuditLogService;
import com.shadiwaley.server.audit.domain.AuditAction;
import com.shadiwaley.server.audit.domain.AuditEntityType;
import com.shadiwaley.server.employee.domain.EmployeeDocumentType;
import com.shadiwaley.server.employee.domain.EmployeeDocumentVerificationStatus;
import com.shadiwaley.server.employee.dto.request.EmployeeDocumentReviewRequest;
import com.shadiwaley.server.employee.dto.request.EmployeeProfileUpsertRequest;
import com.shadiwaley.server.employee.dto.response.EmployeeDocumentResponse;
import com.shadiwaley.server.employee.dto.response.EmployeeProfileResponse;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeAccount;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeDocument;
import com.shadiwaley.server.employee.infrastructure.entity.EmployeeProfile;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeAccountRepository;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeDocumentRepository;
import com.shadiwaley.server.employee.infrastructure.repository.EmployeeProfileRepository;
import com.shadiwaley.server.media.application.storage.FileStorageService;
import com.shadiwaley.server.security.AuthUser;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeProfileService {

    private final EmployeeAccountRepository employeeAccountRepository;
    private final EmployeeProfileRepository employeeProfileRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final EmployeeFileStorageService employeeFileStorageService;

    @Transactional
    public EmployeeProfileResponse upsertProfile(UUID employeeId, EmployeeProfileUpsertRequest request) {
        EmployeeAccount employee = getEmployee(employeeId);

        EmployeeProfile profile = employeeProfileRepository.findByEmployeeAccountId(employeeId)
                .orElseGet(() -> {
                    EmployeeProfile created = new EmployeeProfile();
                    created.setEmployeeAccount(employee);
                    return created;
                });

        if (request.getFirstName() != null) profile.setFirstName(request.getFirstName());
        if (request.getLastName() != null) profile.setLastName(request.getLastName());
        if (request.getGender() != null) profile.setGender(request.getGender());
        if (request.getDateOfBirth() != null) profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getAlternatePhone() != null) profile.setAlternatePhone(request.getAlternatePhone());
        if (request.getPersonalEmail() != null) profile.setPersonalEmail(request.getPersonalEmail());
        if (request.getAddress() != null) profile.setAddress(request.getAddress());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getState() != null) profile.setState(request.getState());
        if (request.getDistrict() != null) profile.setDistrict(request.getDistrict());
        if (request.getPincode() != null) profile.setPincode(request.getPincode());
        if (request.getEmergencyContactName() != null) profile.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getEmergencyContactPhone() != null) profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        if (request.getEmployeeCode() != null) profile.setEmployeeCode(request.getEmployeeCode());
        if (request.getDesignation() != null) profile.setDesignation(request.getDesignation());
        if (request.getEmploymentType() != null) profile.setEmploymentType(request.getEmploymentType());
        if (request.getJoiningDate() != null) profile.setJoiningDate(request.getJoiningDate());
        if (request.getWorkMode() != null) profile.setWorkMode(request.getWorkMode());
        if (request.getAadhaarNumber() != null) profile.setAadhaarNumber(request.getAadhaarNumber());
        if (request.getHighestQualification() != null) profile.setHighestQualification(request.getHighestQualification());
        if (request.getCollegeName() != null) profile.setCollegeName(request.getCollegeName());
        if (request.getTotalExperienceYears() != null) profile.setTotalExperienceYears(request.getTotalExperienceYears());
        if (request.getPreviousCompany() != null) profile.setPreviousCompany(request.getPreviousCompany());

        if (request.getReportingManagerId() != null) {
            EmployeeAccount manager = getEmployee(request.getReportingManagerId());
            profile.setReportingManager(manager);
        }

        EmployeeProfile saved = employeeProfileRepository.save(profile);

        auditLogService.record(
                AuditAction.EMPLOYEE_UPDATED,
                AuditEntityType.EMPLOYEE_ACCOUNT,
                employeeId,
                "Employee profile updated"
        );

        return toProfileResponse(saved);
    }

    @Transactional(readOnly = true)
    public EmployeeProfileResponse getProfile(UUID employeeId) {
        EmployeeProfile profile = employeeProfileRepository.findByEmployeeAccountId(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found"));

        return toProfileResponse(profile);
    }

    @Transactional
    public EmployeeDocumentResponse uploadDocument(
            UUID employeeId,
            EmployeeDocumentType documentType,
            MultipartFile file
    ) {
        EmployeeAccount employee = getEmployee(employeeId);

        String storageKey = employeeFileStorageService.store(file, employeeId.toString());

        EmployeeDocument document = new EmployeeDocument();
        document.setEmployeeAccount(employee);
        document.setDocumentType(documentType);
        document.setOriginalFileName(file.getOriginalFilename());
        document.setStorageKey(storageKey);
        document.setContentType(file.getContentType());
        document.setSizeBytes(file.getSize());

        EmployeeDocument saved = employeeDocumentRepository.save(document);

        auditLogService.record(
                AuditAction.EMPLOYEE_UPDATED,
                AuditEntityType.EMPLOYEE_ACCOUNT,
                employeeId,
                "Employee document uploaded: " + documentType
        );

        return toDocumentResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<EmployeeDocumentResponse> getDocuments(UUID employeeId) {
        getEmployee(employeeId);

        return employeeDocumentRepository
                .findByEmployeeAccountIdAndDeletedFalseOrderByUploadedAtDesc(employeeId)
                .stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    @Transactional
    public EmployeeDocumentResponse reviewDocument(UUID documentId, EmployeeDocumentReviewRequest request) {
        EmployeeDocument document = employeeDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Employee document not found"));

        EmployeeAccount reviewer = employeeAccountRepository.findById(AuthUser.getCurrentActorId())
                .orElse(null);

        document.setVerificationStatus(request.getVerificationStatus());
        document.setRejectedReason(
                request.getVerificationStatus() == EmployeeDocumentVerificationStatus.REJECTED
                        ? request.getRejectedReason()
                        : null
        );
        document.setVerifiedByEmployee(reviewer);
        document.setVerifiedAt(Instant.now());

        EmployeeDocument saved = employeeDocumentRepository.save(document);

        auditLogService.record(
                AuditAction.EMPLOYEE_UPDATED,
                AuditEntityType.EMPLOYEE_ACCOUNT,
                saved.getEmployeeAccount().getId(),
                "Employee document reviewed: " + saved.getVerificationStatus()
        );

        return toDocumentResponse(saved);
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        EmployeeDocument document = employeeDocumentRepository.findByIdAndDeletedFalse(documentId)
                .orElseThrow(() -> new EntityNotFoundException("Employee document not found"));

        document.setDeleted(true);
        document.setDeletedAt(Instant.now());

        employeeDocumentRepository.save(document);
    }

    private EmployeeProfileResponse toProfileResponse(EmployeeProfile profile) {
        EmployeeAccount employee = profile.getEmployeeAccount();

        return EmployeeProfileResponse.builder()
                .employeeId(employee.getId())
                .firstName(profile.getFirstName())
                .lastName(profile.getLastName())
                .gender(profile.getGender())
                .dateOfBirth(profile.getDateOfBirth())
                .alternatePhone(profile.getAlternatePhone())
                .personalEmail(profile.getPersonalEmail())
                .address(profile.getAddress())
                .city(profile.getCity())
                .state(profile.getState())
                .district(profile.getDistrict())
                .pincode(profile.getPincode())
                .emergencyContactName(profile.getEmergencyContactName())
                .emergencyContactPhone(profile.getEmergencyContactPhone())
                .employeeCode(profile.getEmployeeCode())
                .designation(profile.getDesignation())
                .employmentType(profile.getEmploymentType())
                .joiningDate(profile.getJoiningDate())
                .workMode(profile.getWorkMode())
                .reportingManagerId(profile.getReportingManager() != null ? profile.getReportingManager().getId() : null)
                .reportingManagerName(profile.getReportingManager() != null ? profile.getReportingManager().getFullName() : null)
                .aadhaarMasked(maskAadhaar(profile.getAadhaarNumber()))
                .highestQualification(profile.getHighestQualification())
                .collegeName(profile.getCollegeName())
                .totalExperienceYears(profile.getTotalExperienceYears())
                .previousCompany(profile.getPreviousCompany())
                .documents(getDocuments(employee.getId()))
                .build();
    }

    private EmployeeDocumentResponse toDocumentResponse(EmployeeDocument document) {
        return EmployeeDocumentResponse.builder()
                .documentId(document.getId())
                .documentType(document.getDocumentType())
                .originalFileName(document.getOriginalFileName())
                .contentType(document.getContentType())
                .sizeBytes(document.getSizeBytes())
                .verificationStatus(document.getVerificationStatus())
                .rejectedReason(document.getRejectedReason())
                .uploadedAt(document.getUploadedAt())
                .build();
    }

    private String maskAadhaar(String aadhaarNumber) {
        if (aadhaarNumber == null || aadhaarNumber.length() < 4) {
            return null;
        }

        return "XXXX-XXXX-" + aadhaarNumber.substring(aadhaarNumber.length() - 4);
    }

    private EmployeeAccount getEmployee(UUID employeeId) {
        return employeeAccountRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }
}