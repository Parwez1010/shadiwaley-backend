package com.shadiwaley.server.subscription.domain;

import java.math.BigDecimal;
import java.util.List;

public final class PlanCatalog {

    private PlanCatalog() {
    }

    public static List<PlanDefinition> allPlans() {
        return List.of(
                freeOnboarding(),
                sixMonth999(),
                lifetime1999()
        );
    }

    public static PlanDefinition getPlan(PlanType planType) {
        return allPlans()
                .stream()
                .filter(plan -> plan.planType() == planType)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid plan type")
                );
    }

    private static PlanDefinition freeOnboarding() {

        return new PlanDefinition(
                PlanType.FREE_ONBOARDING,
                "Free Onboarding",
                BigDecimal.ZERO,
                "Free",
                0,
                -1,
                -1,
                true,
                true,
                false,
                false,
                false,
                false,
                List.of(
                        "Create family profile",
                        "Complete onboarding",
                        "Browse profiles",
                        "Send and receive rishta requests",
                        "Family chat after rishta acceptance",
                        "Upload profile documents",
                        "CRM verification eligible"
                )
        );
    }

    private static PlanDefinition sixMonth999() {

        return new PlanDefinition(
                PlanType.SIX_MONTH_999,
                "Premium",
                new BigDecimal("999.00"),
                "6 Months",
                180,
                -1,
                3,
                true,
                true,
                true,
                true,
                true,
                false,
                List.of(
                        "Unlimited profile browsing",
                        "Unlimited rishta requests",
                        "3 autopilot profiles per week",
                        "Dedicated CRM support",
                        "Basic meeting coordination"
                )
        );
    }

    private static PlanDefinition lifetime1999() {

        return new PlanDefinition(
                PlanType.LIFETIME_1999,
                "Elite",
                new BigDecimal("1999.00"),
                "Lifetime",
                0,
                -1,
                5,
                true,
                true,
                true,
                true,
                true,
                true,
                List.of(
                        "Unlimited profile browsing",
                        "Unlimited rishta requests",
                        "5 autopilot profiles per week",
                        "Senior CRM support",
                        "Full meeting coordination",
                        "Priority profile review"
                )
        );
    }
}