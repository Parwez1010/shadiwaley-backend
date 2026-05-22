package com.shadiwaley.server.autopilot.domain;

public enum AutopilotQueueStatus {
    PENDING,
    READY,
    DISPATCHED,
    SKIPPED,
    POSTPONED,
    BLOCKED,
    COMPLETED
}