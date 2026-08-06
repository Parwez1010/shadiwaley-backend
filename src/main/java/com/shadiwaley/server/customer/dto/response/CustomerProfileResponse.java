package com.shadiwaley.server.customer.dto.response;

import com.shadiwaley.server.parent.domain.ParentRelation;
import com.shadiwaley.server.profile.domain.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Builder
public class CustomerProfileResponse {

    private UUID userId;
    private UUID profileId;
    private String phone;
    private String side;

    private ProfileStatus profileStatus;
    private Short completionPct;
    private boolean readyForReview;
    private List<?> missingFields;

    private ParentInfo parent;
    private ProfileInfo profile;
    private PreferenceInfo preferences;

    @Getter
    @Builder
    public static class ParentInfo {
        private String parentName;
        private ParentRelation parentRelation;
        private String parentPhone;
        private String district;
        private String state;
        private String maslak;
        private String imamReference;
        private String masjidName;
        private String caste;
    }

    @Getter
    @Builder
    public static class ProfileInfo {
        private String candidateFirstName;
        private Short candidateAge;
        private Short candidateHeightCm;
        private String education;
        private String religion;
        private MaritalStatus maritalStatus;
        private String quranLevel;
        private String namaazRegularity;
        private Boolean previouslyMarried;
        private String professionType;
        private String professionTitle;
        private Integer monthlyIncome;
        private Integer mehrOffered;
        private Integer mehrMinimumExpected;
        private String houseType;
        private String familyType;
        private String expectationsText;

        // NEW
        private LocalDate dateOfBirth;
        private String bloodGroup;
        private String complexion;
        private BodyType bodyType;
        private String motherTongue;
        private Set<String> languagesKnown;
        private String sect;
        private Diet diet;
        private Boolean smoker;
        private Boolean drinker;
        private ExerciseFrequency exerciseFrequency;
        private Boolean wearsHijab;
        private FamilyStatus familyStatus;
        private FamilyValues familyValues;
        private Short brothersCount;
        private Short sistersCount;
        private Set<String> interests;
    }

    @Getter
    @Builder
    public static class PreferenceInfo {
        private String preferredMaslak;
        private String preferredState;
        private String preferredDistrict;
        private String preferredCaste;
        private Short minAge;
        private Short maxAge;
        private String preferredEducation;
        private String preferredFamilyType;
        private boolean requireImamRef;
        private boolean requireIdVerified;
    }
}