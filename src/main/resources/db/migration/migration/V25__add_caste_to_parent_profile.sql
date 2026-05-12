ALTER TABLE parent_profile
ADD COLUMN IF NOT EXISTS caste VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_parent_profile_caste
ON parent_profile(caste);

ALTER TABLE user_preferences
ADD COLUMN IF NOT EXISTS preferred_caste VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_user_preferences_preferred_caste
ON user_preferences(preferred_caste);