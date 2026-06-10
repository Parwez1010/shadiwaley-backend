package com.shadiwaley.server.customer.dto.response;

import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CustomerMeResponse {

    private UUID userId;
    private UUID profileId;

    private UserSide side;

    private String phone;

    private String parentName;
    private String candidateName;

    private String profileStatus;
    private Short completionPct;

    private String planCode;
    private String planName;
    private String subscriptionStatus;

    private long unreadNotificationsCount;
    private long unreadChatCount;
}