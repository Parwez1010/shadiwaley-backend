ALTER TABLE user_profile
ADD COLUMN IF NOT EXISTS marital_status VARCHAR(40);

ALTER TABLE user_profile
ADD COLUMN IF NOT EXISTS religion VARCHAR(50) DEFAULT 'ISLAM';

ALTER TABLE parent_profile
ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'India';

CREATE INDEX IF NOT EXISTS idx_user_profile_status_age
ON user_profile(profile_status, candidate_age);

CREATE INDEX IF NOT EXISTS idx_user_profile_marital_status
ON user_profile(marital_status);

CREATE INDEX IF NOT EXISTS idx_user_profile_religion
ON user_profile(religion);

CREATE INDEX IF NOT EXISTS idx_parent_profile_location
ON parent_profile(state, district, maslak);