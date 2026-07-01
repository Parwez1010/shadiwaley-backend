package com.shadiwaley.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AdminChatParticipantResponse {

    private UUID userId;

    private UUID profileId;

    private String displayId;

    private String candidateName;

    private String parentName;

    private String phone;

    private String side;

    private String district;

    private String state;

    private UUID familyUserId;
    private String parentPhone;
    private Short age;
    private String profilePhotoViewUrl;

    private String maslak;

    private String caste;

    private String education;

    private String professionTitle;
}