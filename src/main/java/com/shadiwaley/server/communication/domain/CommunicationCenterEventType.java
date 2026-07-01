package com.shadiwaley.server.communication.domain;

public final class CommunicationCenterEventType {

    private CommunicationCenterEventType() {
    }

    public static final String SUPPORT_TICKET_CREATED = "SUPPORT_TICKET_CREATED";
    public static final String SUPPORT_TICKET_ASSIGNED = "SUPPORT_TICKET_ASSIGNED";
    public static final String SUPPORT_TICKET_REPLIED = "SUPPORT_TICKET_REPLIED";
    public static final String SUPPORT_TICKET_STATUS_CHANGED = "SUPPORT_TICKET_STATUS_CHANGED";

    public static final String CHAT_ROOM_ASSIGNED = "CHAT_ROOM_ASSIGNED";
    public static final String CHAT_ROOM_CLOSED = "CHAT_ROOM_CLOSED";
    public static final String CHAT_NEEDS_ATTENTION = "CHAT_NEEDS_ATTENTION";
    public static final String CHAT_ATTENTION_REMOVED = "CHAT_ATTENTION_REMOVED";

    public static final String BULK_ACTION_COMPLETED = "BULK_ACTION_COMPLETED";
    public static final String CHAT_CRM_MESSAGE_SENT = "CHAT_CRM_MESSAGE_SENT";
}