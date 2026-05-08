package com.shadiwaley.server.media.domain;

public enum MediaVisibility {

    /*
     * Only owner family can access
     */
    PRIVATE,

    /*
     * Admin + CRM + verifier only
     */
    INTERNAL_ONLY,

    /*
     * Can be shared in approved parent-to-parent chats
     */
    CHAT_SHAREABLE,

    /*
     * Can be used in WhatsApp autopilot dispatch
     */
    AUTOPILOT_SHAREABLE
}