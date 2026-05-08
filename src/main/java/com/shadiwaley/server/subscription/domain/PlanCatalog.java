package com.shadiwaley.server.subscription.domain;

import java.util.List;

public final class PlanCatalog {

    private PlanCatalog() {
    }

    public static List<PlanDefinition> allPlans() {
        return List.of(
                freeOnboarding(),
                basic(),
                premium(),
                elite()
        );
    }

    public static PlanDefinition getPlan(PlanType planType) {
        return allPlans()
                .stream()
                .filter(plan -> plan.planType() == planType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid plan type"));
    }

    private static PlanDefinition freeOnboarding() {
        return new PlanDefinition(
                PlanType.FREE_ONBOARDING,
                "Free Onboarding",
                0,
                "Free",
                30,
                0,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                List.of(
                        "Create family profile",
                        "Complete onboarding",
                        "Upload profile documents",
                        "CRM verification eligible",
                        "Limited dashboard access"
                )
        );
    }

    private static PlanDefinition basic() {
        return new PlanDefinition(
                PlanType.BASIC_299,
                "Basic ₹299",
                29900,
                "Monthly",
                30,
                5,
                0,
                true,
                true,
                false,
                false,
                false,
                false,
                List.of(
                        "Unlimited profile browsing",
                        "5 rishta requests per month",
                        "Family chat after rishta acceptance",
                        "Standard profile review"
                )
        );
    }

    private static PlanDefinition premium() {
        return new PlanDefinition(
                PlanType.PREMIUM_999,
                "Premium ₹999",
                99900,
                "Monthly",
                30,
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

    private static PlanDefinition elite() {
        return new PlanDefinition(
                PlanType.ELITE_2499,
                "Elite ₹2499",
                249900,
                "One-time",
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