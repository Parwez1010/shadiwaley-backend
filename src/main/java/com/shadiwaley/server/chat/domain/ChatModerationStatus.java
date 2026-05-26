package com.shadiwaley.server.chat.domain;

public enum ChatModerationStatus {
    CLEAN,
    FLAGGED,
    HIDDEN,
    DELETED,
    REPORTED,
    UNDER_REVIEW,

    // legacy support
    VISIBLE
}