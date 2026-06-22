package com.shadiwaley.server.subscription.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.subscription.application.service.SubscriptionService;
import com.shadiwaley.server.subscription.dto.request.SelectPlanRequest;
import com.shadiwaley.server.subscription.dto.response.PlanResponse;
import com.shadiwaley.server.subscription.dto.response.SubscriptionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/plans")
    public ApiResponse<List<PlanResponse>> getPlans() {
        return ResponseFactory.success("Plans fetched successfully", subscriptionService.getPlans());
    }

    @PostMapping("/select-plan")
    public ApiResponse<SubscriptionResponse> selectPlan(@Valid @RequestBody SelectPlanRequest request) {
        return ResponseFactory.success("Plan selected successfully", subscriptionService.selectPlan(request));
    }

    @GetMapping("/my-plan")
    public ApiResponse<SubscriptionResponse> getMyPlan() {
        return ResponseFactory.success("Current plan fetched successfully", subscriptionService.getMyPlan());
    }

    @GetMapping("/me")
    public ApiResponse<SubscriptionResponse> getMe() {
        return ResponseFactory.success(
                "Current subscription fetched successfully",
                subscriptionService.getMyPlan()
        );
    }

    @PostMapping("/request-upgrade")
    public ApiResponse<SubscriptionResponse> requestUpgrade(
            @Valid @RequestBody SelectPlanRequest request
    ) {
        return ResponseFactory.success(
                "Subscription upgrade request submitted successfully",
                subscriptionService.requestUpgrade(request)
        );
    }
}