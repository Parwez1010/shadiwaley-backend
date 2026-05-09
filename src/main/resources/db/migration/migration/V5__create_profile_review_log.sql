CREATE TABLE profile_review_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_profile_id UUID NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,

    reviewer_user_id UUID,

    action VARCHAR(40) NOT NULL,
    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_profile_review_profile_id
ON profile_review_log(user_profile_id);

CREATE INDEX idx_profile_review_action
ON profile_review_log(action);