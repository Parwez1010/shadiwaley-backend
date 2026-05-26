package com.shadiwaley.server.chat.domain;

public enum ChatRoomStatus {
    ACTIVE,
    PENDING_RESPONSE,
    NEEDS_CRM_ATTENTION,
    REPORTED,
    BLOCKED,

    CLOSED_SUCCESS,
    CLOSED_REJECTED,
    CLOSED_NO_RESPONSE,
    CLOSED_BY_ADMIN,

    // legacy support for existing customer close flow
    CLOSED
}