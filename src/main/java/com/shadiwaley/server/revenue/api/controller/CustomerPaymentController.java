package com.shadiwaley.server.revenue.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.revenue.application.service.RazorpayPaymentService;
import com.shadiwaley.server.revenue.application.service.RevenueService;
import com.shadiwaley.server.revenue.dto.request.CreateRazorpayOrderRequest;
import com.shadiwaley.server.revenue.dto.request.VerifyRazorpayPaymentRequest;
import com.shadiwaley.server.revenue.dto.response.*;
import com.shadiwaley.server.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customer/payments")
@RequiredArgsConstructor
public class CustomerPaymentController {

    private final RazorpayPaymentService razorpayPaymentService;
    private final RevenueService revenueService;

    @GetMapping("/plans")
    public ApiResponse<List<RevenuePlanResponse>> plans() {
        return ResponseFactory.success(
                "Plans fetched successfully",
                revenueService.getPlans()
        );
    }

    @PostMapping("/razorpay/order")
    public ApiResponse<RazorpayOrderResponse> createRazorpayOrder(
            @Valid @RequestBody CreateRazorpayOrderRequest request
    ) throws Exception {
        return ResponseFactory.success(
                "Razorpay order created successfully",
                razorpayPaymentService.createOrder(request)
        );
    }

    @PostMapping("/razorpay/verify")
    public ApiResponse<RazorpayPaymentVerifyResponse> verifyRazorpayPayment(
            @Valid @RequestBody VerifyRazorpayPaymentRequest request
    ) {
        return ResponseFactory.success(
                "Payment verified successfully",
                razorpayPaymentService.verifyPayment(request)
        );
    }

    @GetMapping("/subscription")
    public ApiResponse<FamilySubscriptionSummaryResponse> subscription() {
        return ResponseFactory.success(
                "Subscription fetched successfully",
                revenueService.getMySubscription(AuthUser.getCurrentUserId())
        );
    }

    @GetMapping("/history")
    public ApiResponse<PaymentHistoryPageResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseFactory.success(
                "Payment history fetched successfully",
                revenueService.getMyPayments(
                        AuthUser.getCurrentUserId(),
                        page,
                        size
                )
        );
    }


}