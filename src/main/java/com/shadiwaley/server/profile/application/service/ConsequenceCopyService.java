package com.shadiwaley.server.profile.application.service;

import org.springframework.stereotype.Service;

@Service
public class ConsequenceCopyService {

    public String getMessage(String field) {
        return switch (field) {
            case "PROFILE_PHOTO" ->
                    "Add a candidate photo to build trust. Profiles with a clear photo are easier for families and CRM to review.";

            case "ID_PROOF" ->
                    "Upload ID proof so our team can verify the profile and move it closer to approval.";

            case "INCOME_PROOF" ->
                    "Upload income proof to support the earning details. This helps serious families make confident decisions.";

            case "IMAM_REFERENCE" ->
                    "Add an Imam reference to strengthen profile credibility and help families trust the details.";

            case "PARENT_NAME" ->
                    "Add the parent name so our team knows who is representing the family.";

            case "PARENT_RELATION" ->
                    "Select the parent relation to avoid confusion during CRM verification.";

            case "DISTRICT" ->
                    "Add the district so we can suggest more relevant local matches.";

            case "MASLAK" ->
                    "Select the Maslak so match recommendations can be more accurate.";

            case "CANDIDATE_NAME" ->
                    "Add the candidate name so the profile can be reviewed properly.";

            case "CANDIDATE_AGE" ->
                    "Add the candidate age so age-based matching can work correctly.";

            case "EDUCATION" ->
                    "Add education details so families can understand the candidate background better.";

            case "QURAN_LEVEL" ->
                    "Add Quran learning details to help families understand religious compatibility.";

            case "NAMAAZ_REGULARITY" ->
                    "Add prayer regularity details to make religious expectations clear.";

            case "MEHR_OFFERED" ->
                    "Add the Mehr offered so the girl side family can evaluate the proposal clearly.";

            case "MEHR_EXPECTED" ->
                    "Add the expected Mehr so the boy side family can understand expectations clearly.";

            default ->
                    "Complete this field to improve profile quality, verification speed, and match accuracy.";
        };
    }
}