package com.shadiwaley.server.match.domain;

public enum DispatchReadiness {
    READY_TO_SEND,
    NEEDS_REVIEW,
    LOW_CONFIDENCE,
    ALREADY_DISPATCHED
}