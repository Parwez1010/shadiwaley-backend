CREATE INDEX IF NOT EXISTS idx_user_profile_status_created
ON user_profile(profile_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_profile_completion
ON user_profile(completion_pct);

CREATE INDEX IF NOT EXISTS idx_user_profile_age
ON user_profile(candidate_age);

CREATE INDEX IF NOT EXISTS idx_user_profile_education
ON user_profile(education);

CREATE INDEX IF NOT EXISTS idx_user_profile_profession
ON user_profile(profession_type);

CREATE INDEX IF NOT EXISTS idx_user_profile_family_type
ON user_profile(family_type);

CREATE INDEX IF NOT EXISTS idx_parent_profile_match_filters
ON parent_profile(district, state, caste, maslak);

CREATE INDEX IF NOT EXISTS idx_proposal_pair_status
ON proposal(from_profile_id, to_profile_id, status);

CREATE INDEX IF NOT EXISTS idx_proposal_reverse_pair_status
ON proposal(to_profile_id, from_profile_id, status);

CREATE INDEX IF NOT EXISTS idx_media_profile_type_primary
ON media_file(user_profile_id, media_type, is_primary, is_deleted);