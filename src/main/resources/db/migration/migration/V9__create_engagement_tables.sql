CREATE TABLE engagement_loop_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL UNIQUE
        REFERENCES user_account(id) ON DELETE CASCADE,

    current_streak INTEGER NOT NULL DEFAULT 0,
    momentum_score INTEGER NOT NULL DEFAULT 0,
    engagement_score INTEGER NOT NULL DEFAULT 0,

    last_login_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,

    profile_views INTEGER NOT NULL DEFAULT 0,
    profile_shares INTEGER NOT NULL DEFAULT 0,

    onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE,
    profile_live BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_engagement_user
ON engagement_loop_state(user_account_id);

CREATE TABLE profile_milestone (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL
        REFERENCES user_account(id) ON DELETE CASCADE,

    milestone_code VARCHAR(80) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(300),

    achieved BOOLEAN NOT NULL DEFAULT FALSE,

    achieved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_milestone_user
ON profile_milestone(user_account_id);

CREATE INDEX idx_milestone_code
ON profile_milestone(milestone_code);