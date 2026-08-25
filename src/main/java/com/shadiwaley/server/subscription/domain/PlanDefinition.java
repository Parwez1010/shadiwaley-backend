package com.shadiwaley.server.subscription.domain;

import java.math.BigDecimal;
import java.util.List;


public record PlanDefinition(
        PlanType planType,
        String displayName,
        BigDecimal price,
        String billingLabel,
        int durationDays,
        int rishtaRequestsPerMonth,
        int profilesPerWeek,
        boolean browseProfiles,
        boolean familyChat,
        boolean autopilotDispatch,
        boolean dedicatedCrm,
        boolean meetingCoordination,
        boolean priorityProfileReview,
        List<String> features
) {
}