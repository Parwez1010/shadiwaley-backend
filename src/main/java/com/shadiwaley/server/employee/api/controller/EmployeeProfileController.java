package com.shadiwaley.server.employee.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.employee.application.service.EmployeeProfileService;
import com.shadiwaley.server.employee.domain.EmployeeDocumentType;
import com.shadiwaley.server.employee.dto.request.EmployeeDocumentReviewRequest;
import com.shadiwaley.server.employee.dto.request.EmployeeProfileUpsertRequest;
import com.shadiwaley.server.employee.dto.response.EmployeeDocumentResponse;
import com.shadiwaley.server.employee.dto.response.EmployeeProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
public class EmployeeProfileController {

    private final EmployeeProfileService employeeProfileService;

    @PatchMapping("/{employeeId}/profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<EmployeeProfileResponse> upsertProfile(
            @PathVariable UUID employeeId,
            @Valid @RequestBody EmployeeProfileUpsertRequest request
    ) {
        return ResponseFactory.success(
                "Employee profile updated successfully",
                employeeProfileService.upsertProfile(employeeId, request)
        );
    }

    @GetMapping("/{employeeId}/profile")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<EmployeeProfileResponse> getProfile(@PathVariable UUID employeeId) {
        return ResponseFactory.success(
                "Employee profile fetched successfully",
                employeeProfileService.getProfile(employeeId)
        );
    }

    @PostMapping("/{employeeId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<EmployeeDocumentResponse> uploadDocument(
            @PathVariable UUID employeeId,
            @RequestParam EmployeeDocumentType documentType,
            @RequestPart MultipartFile file
    ) {
        return ResponseFactory.success(
                "Employee document uploaded successfully",
                employeeProfileService.uploadDocument(employeeId, documentType, file)
        );
    }

    @GetMapping("/{employeeId}/documents")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<List<EmployeeDocumentResponse>> documents(@PathVariable UUID employeeId) {
        return ResponseFactory.success(
                "Employee documents fetched successfully",
                employeeProfileService.getDocuments(employeeId)
        );
    }

    @PatchMapping("/documents/{documentId}/review")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ApiResponse<EmployeeDocumentResponse> reviewDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody EmployeeDocumentReviewRequest request
    ) {
        return ResponseFactory.success(
                "Employee document reviewed successfully",
                employeeProfileService.reviewDocument(documentId, request)
        );
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID documentId) {
        employeeProfileService.deleteDocument(documentId);

        return ResponseFactory.success(
                "Employee document deleted successfully",
                null
        );
    }
}