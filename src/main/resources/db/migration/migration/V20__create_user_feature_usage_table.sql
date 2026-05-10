CREATE TABLE user_feature_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL
        REFERENCES user_account(id) ON DELETE CASCADE,

    usage_date DATE NOT NULL,

    rishta_requests_sent INTEGER NOT NULL DEFAULT 0,
    profile_views INTEGER NOT NULL DEFAULT 0,
    active_chat_rooms INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(user_account_id, usage_date)
);

CREATE INDEX idx_feature_usage_user_date
ON user_feature_usage(user_account_id, usage_date);