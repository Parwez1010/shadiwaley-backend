package com.shadiwaley.server.customer.api.controller;

import com.shadiwaley.server.common.response.ApiResponse;
import com.shadiwaley.server.common.response.ResponseFactory;
import com.shadiwaley.server.customer.application.service.CustomerAppService;
import com.shadiwaley.server.customer.dto.response.CustomerBootstrapResponse;
import com.shadiwaley.server.customer.dto.response.CustomerDashboardResponse;
import com.shadiwaley.server.customer.dto.response.CustomerMeResponse;
import com.shadiwaley.server.customer.dto.response.CustomerNavigationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
public class CustomerAppController {

    private final CustomerAppService customerAppService;

    @GetMapping("/me")
    public ApiResponse<CustomerMeResponse> me() {
        return ResponseFactory.success(
                "Customer profile fetched successfully",
                customerAppService.getMe()
        );
    }

    @GetMapping("/dashboard")
    public ApiResponse<CustomerDashboardResponse> dashboard() {
        return ResponseFactory.success(
                "Customer dashboard fetched successfully",
                customerAppService.getDashboard()
        );
    }

    @GetMapping("/navigation")
    public ApiResponse<CustomerNavigationResponse> navigation() {
        return ResponseFactory.success(
                "Customer navigation fetched successfully",
                customerAppService.getNavigation()
        );
    }

    @GetMapping("/bootstrap")
    public ApiResponse<CustomerBootstrapResponse> bootstrap() {
        return ResponseFactory.success(
                "Customer bootstrap fetched successfully",
                customerAppService.getBootstrap()
        );
    }


}