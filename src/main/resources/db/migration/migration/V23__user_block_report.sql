CREATE TABLE user_block (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    blocker_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    blocked_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,

    reason VARCHAR(300),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(blocker_user_id, blocked_user_id)
);

CREATE INDEX idx_user_block_blocker
ON user_block(blocker_user_id);

CREATE INDEX idx_user_block_blocked
ON user_block(blocked_user_id);

CREATE TABLE user_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    reporter_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,

    reason VARCHAR(100) NOT NULL,
    details VARCHAR(500),

    status VARCHAR(40) NOT NULL DEFAULT 'OPEN',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_report_reported
ON user_report(reported_user_id);

CREATE INDEX idx_user_report_status
ON user_report(status);