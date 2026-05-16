package com.shadiwaley.server.crm.domain;

public enum CrmTimelineEventType {
    CASE_CREATED,
    CASE_UPDATED,
    CRM_ASSIGNED,
    STAGE_CHANGED,
    PRIORITY_CHANGED,
    NOTE_ADDED,
    FOLLOW_UP_SCHEDULED,
    FOLLOW_UP_COMPLETED,
    FOLLOW_UP_RESCHEDULED,
    CASE_CLOSED,
    CASE_REOPENED
}