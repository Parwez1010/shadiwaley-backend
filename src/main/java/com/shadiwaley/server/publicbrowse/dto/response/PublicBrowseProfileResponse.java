package com.shadiwaley.server.publicbrowse.dto.response;

import com.shadiwaley.server.user.domain.UserSide;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class PublicBrowseProfileResponse {

    private UUID profileId;
    private String displayId;
    private UserSide side;

    private Short candidateAge;

    private String district;
    private String state;
    private String maslak;

    private String education;
    private String quranLevel;
    private String namaazRegularity;

    private String professionType;
    private String familyType;

    private Integer mehrOffered;
    private Integer mehrMinimumExpected;

    private String expectationsText;

    private boolean hasApprovedPhoto;

    private Boolean saved;
    private String proposalStatus;

    private PublicMatchResponse match;

    @Getter
    @Builder
    public static class PublicMatchResponse {
        private Integer totalScore;
    }
}