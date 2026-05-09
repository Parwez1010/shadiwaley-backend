CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE user_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone VARCHAR(15) NOT NULL UNIQUE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BOY', 'GIRL')),
    role VARCHAR(30) NOT NULL DEFAULT 'USER',
    account_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    refresh_token_hash VARCHAR(500),
    refresh_token_expires_at TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE otp_session (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    temp_token UUID NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL,
    side VARCHAR(10) NOT NULL CHECK (side IN ('BOY', 'GIRL')),
    otp_code VARCHAR(10) NOT NULL,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    attempt_count INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE parent_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL UNIQUE REFERENCES user_account(id) ON DELETE CASCADE,
    parent_name VARCHAR(150),
    parent_relation VARCHAR(40),
    parent_phone VARCHAR(15),
    district VARCHAR(100),
    state VARCHAR(100),
    maslak VARCHAR(50),
    imam_reference VARCHAR(200),
    masjid_name VARCHAR(200),
    consent_recorded_at TIMESTAMPTZ,
    consent_type VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_profile (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL UNIQUE REFERENCES user_account(id) ON DELETE CASCADE,
    display_id VARCHAR(20) UNIQUE,
    candidate_first_name VARCHAR(100),
    candidate_age SMALLINT CHECK (candidate_age IS NULL OR candidate_age BETWEEN 18 AND 60),
    candidate_height_cm SMALLINT,
    education VARCHAR(50),
    quran_level VARCHAR(50),
    namaaz_regularity VARCHAR(50),
    previously_married BOOLEAN,
    profession_type VARCHAR(80),
    profession_title VARCHAR(150),
    monthly_income INTEGER,
    mehr_offered INTEGER,
    mehr_minimum_expected INTEGER,
    house_type VARCHAR(50),
    family_type VARCHAR(50),
    expectations_text VARCHAR(500),
    profile_status VARCHAR(40) NOT NULL DEFAULT 'INCOMPLETE',
    completion_pct SMALLINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE user_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_profile_id UUID NOT NULL UNIQUE REFERENCES user_profile(id) ON DELETE CASCADE,
    preferred_maslak VARCHAR(50),
    preferred_state VARCHAR(100),
    preferred_district VARCHAR(100),
    min_age SMALLINT,
    max_age SMALLINT,
    preferred_education VARCHAR(50),
    preferred_family_type VARCHAR(50),
    require_imam_ref BOOLEAN NOT NULL DEFAULT FALSE,
    require_id_verified BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE autopilot_preference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL UNIQUE REFERENCES user_account(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    whatsapp_number VARCHAR(15),
    photo_consent_level VARCHAR(30) NOT NULL DEFAULT 'AFTER_ACCEPT',
    preferred_dispatch_day VARCHAR(20) NOT NULL DEFAULT 'TUESDAY',
    profiles_per_week SMALLINT NOT NULL DEFAULT 3,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_account_phone ON user_account(phone);
CREATE INDEX idx_otp_session_temp_token ON otp_session(temp_token);
CREATE INDEX idx_otp_session_phone ON otp_session(phone);
CREATE INDEX idx_user_profile_status ON user_profile(profile_status);