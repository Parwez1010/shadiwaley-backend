package com.shadiwaley.server.revenue.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.revenue.application.service.RazorpayPaymentService;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.domain.PaymentMode;
import com.shadiwaley.server.revenue.domain.RevenuePaymentStatus;
import com.shadiwaley.server.revenue.dto.request.AdminCreateRazorpayOrderRequest;
import com.shadiwaley.server.revenue.dto.request.AssignPlanRequest;
import com.shadiwaley.server.revenue.dto.request.ManualPaymentRequest;
import com.shadiwaley.server.revenue.dto.request.VerifyRazorpayPaymentRequest;
import com.shadiwaley.server.revenue.dto.response.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/revenue")
@RequiredArgsConstructor
public class RevenueController {

    private final RevenueService revenueService;
    private final RazorpayPaymentService razorpayPaymentService;

    @GetMapping("/plans")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<List<RevenuePlanResponse>> getPlans() {
        return ResponseFactory.success(
                "Plans fetched successfully",
                revenueService.getPlans()
        );
    }

    @PostMapping("/subscriptions/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<SubscriptionResponse> assignPlan(
            @Valid @RequestBody AssignPlanRequest request
    ) {
        return ResponseFactory.success(
                "Plan assigned successfully",
                revenueService.assignPlan(request)
        );
    }

    @PostMapping("/payments/manual")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<ManualPaymentResponse> recordManualPayment(
            @Valid @RequestBody ManualPaymentRequest request
    ) {
        return ResponseFactory.success(
                "Manual payment recorded successfully",
                revenueService.recordManualPayment(request)
        );
    }
    @GetMapping("/families/{userId}/subscription")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<FamilySubscriptionSummaryResponse> getFamilySubscription(
            @PathVariable UUID userId
    ) {
        return ResponseFactory.success(
                "Family subscription fetched successfully",
                revenueService.getFamilySubscription(userId)
        );
    }
    @GetMapping("/families/{userId}/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<PaymentHistoryPageResponse> getFamilyPayments(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RevenuePaymentStatus status,
            @RequestParam(required = false) PaymentMode paymentMode
    ) {
        return ResponseFactory.success(
                "Payment history fetched successfully",
                revenueService.getFamilyPayments(userId, page, size, status, paymentMode)
        );
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<RevenueDashboardResponse> getDashboard(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ResponseFactory.success(
                "Revenue dashboard fetched successfully",
                revenueService.getRevenueDashboard(fromDate, toDate)
        );
    }


    @GetMapping("/payments")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    public ApiResponse<RevenuePaymentListPageResponse> getPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) RevenuePaymentStatus status,
            @RequestParam(required = false) PaymentMode paymentMode,
            @RequestParam(required = false) String planCode,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ResponseFactory.success(
                "Revenue payments fetched successfully",
                revenueService.getRevenuePayments(
                        page,
                        size,
                        status,
                        paymentMode,
                        planCode,
                        search,
                        fromDate,
                        toDate
                )
        );
    }

    @PostMapping("/razorpay/order")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<RazorpayOrderResponse> createAdminRazorpayOrder(
            @Valid @RequestBody AdminCreateRazorpayOrderRequest request
    ) throws Exception {
        return ResponseFactory.success(
                "Razorpay order created successfully",
                razorpayPaymentService.createAdminOrder(request)
        );
    }

    @PostMapping("/razorpay/verify")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'CRM_AGENT')")
    public ApiResponse<RazorpayPaymentVerifyResponse> verifyAdminRazorpayPayment(
            @Valid @RequestBody VerifyRazorpayPaymentRequest request
    ) {
        return ResponseFactory.success(
                "Payment verified successfully",
                razorpayPaymentService.verifyAdminPayment(request)
        );
    }




}