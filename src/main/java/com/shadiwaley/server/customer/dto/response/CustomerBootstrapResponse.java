package com.shadiwaley.server.customer.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerBootstrapResponse {

    private CustomerMeResponse me;

    private CustomerNavigationResponse navigation;

    private CustomerDashboardResponse dashboard;
}