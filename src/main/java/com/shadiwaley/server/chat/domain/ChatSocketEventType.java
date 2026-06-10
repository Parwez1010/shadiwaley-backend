package com.shadiwaley.server.chat.domain;

public final class ChatSocketEventType {

    private ChatSocketEventType() {
    }

    public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    public static final String MESSAGE_EDITED = "MESSAGE_EDITED";
    public static final String MESSAGE_DELETED = "MESSAGE_DELETED";
    public static final String TYPING = "TYPING";
    public static final String READ_RECEIPT = "READ_RECEIPT";
    public static final String ROOM_STATUS_CHANGED = "ROOM_STATUS_CHANGED";
    public static final String CRM_ASSISTED_MESSAGE = "CRM_ASSISTED_MESSAGE";
    public static final String USER_ONLINE = "USER_ONLINE";
    public static final String USER_OFFLINE = "USER_OFFLINE";
    public static final String LAST_SEEN_UPDATED = "LAST_SEEN_UPDATED";

    public static final String ADMIN_NEW_CHAT_MESSAGE = "ADMIN_NEW_CHAT_MESSAGE";
    public static final String ADMIN_CRM_MESSAGE_SENT = "ADMIN_CRM_MESSAGE_SENT";
    public static final String ADMIN_ROOM_NEEDS_ATTENTION = "ADMIN_ROOM_NEEDS_ATTENTION";
    public static final String ADMIN_ROOM_STATUS_CHANGED = "ADMIN_ROOM_STATUS_CHANGED";
    public static final String ADMIN_ROOM_ASSIGNED = "ADMIN_ROOM_ASSIGNED";
}