CREATE TABLE user_notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL
        REFERENCES user_account(id) ON DELETE CASCADE,

    type VARCHAR(60) NOT NULL,

    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,

    action_url VARCHAR(300),
    reference_id UUID,

    read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_user_created
ON user_notification(user_account_id, created_at DESC);

CREATE INDEX idx_notification_user_read
ON user_notification(user_account_id, read);