CREATE TABLE IF NOT EXISTS saved_profile (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    saved_profile_id UUID NOT NULL REFERENCES user_profile(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uk_saved_profile_user_profile
        UNIQUE (user_account_id, saved_profile_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_profile_user
ON saved_profile(user_account_id);

CREATE INDEX IF NOT EXISTS idx_saved_profile_profile
ON saved_profile(saved_profile_id);