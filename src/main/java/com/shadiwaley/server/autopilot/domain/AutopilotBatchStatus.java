package com.shadiwaley.server.autopilot.domain;

public enum AutopilotBatchStatus {
    DRAFT,
    READY,
    SENT,
    PARTIALLY_RESPONDED,
    RESPONDED,
    SKIPPED,
    FAILED,
    CANCELLED
}