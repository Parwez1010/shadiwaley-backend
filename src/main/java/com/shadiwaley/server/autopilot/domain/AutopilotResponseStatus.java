package com.shadiwaley.server.autopilot.domain;

public enum AutopilotResponseStatus {
    NO_RESPONSE,
    INTERESTED,
    NOT_INTERESTED,
    CALL_BACK,
    ASKED_FOR_MORE_DETAILS,
    ACCEPTED,
    REJECTED,
    WRONG_CONTACT,
    FOLLOW_UP_REQUIRED
}