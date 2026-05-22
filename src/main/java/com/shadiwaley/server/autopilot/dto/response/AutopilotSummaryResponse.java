package com.shadiwaley.server.autopilot.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AutopilotSummaryResponse {

    private long totalQueue;

    private long ready;
    private long pending;
    private long dispatchedToday;
    private long overdue;
    private long blocked;

    private long responsesPending;
    private long interestedResponses;
    private long acceptedResponses;

    private long skippedThisWeek;

    private long premiumDue;
    private long freeDue;
}