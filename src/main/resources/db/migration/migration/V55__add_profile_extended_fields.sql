-- V{next}__add_profile_extended_fields.sql

ALTER TABLE user_profile
    ADD COLUMN date_of_birth      DATE,
    ADD COLUMN blood_group        VARCHAR(5),
    ADD COLUMN complexion         VARCHAR(30),
    ADD COLUMN body_type          VARCHAR(30),
    ADD COLUMN mother_tongue      VARCHAR(50),
    ADD COLUMN sect               VARCHAR(50),
    ADD COLUMN diet               VARCHAR(30),
    ADD COLUMN is_smoker          BOOLEAN,
    ADD COLUMN is_drinker         BOOLEAN,
    ADD COLUMN exercise_frequency VARCHAR(30),
    ADD COLUMN wears_hijab        BOOLEAN,
    ADD COLUMN family_status      VARCHAR(30),
    ADD COLUMN family_values      VARCHAR(30),
    ADD COLUMN brothers_count     SMALLINT,
    ADD COLUMN sisters_count      SMALLINT;

CREATE TABLE user_profile_language (
    user_profile_id UUID NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    language        VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_profile_id, language)
);

CREATE TABLE user_profile_interest (
    user_profile_id UUID NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,
    interest        VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_profile_id, interest)
);