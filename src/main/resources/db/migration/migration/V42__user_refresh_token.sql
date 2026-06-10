CREATE TABLE IF NOT EXISTS user_refresh_token (
    id UUID PRIMARY KEY,
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    token_hash VARCHAR(500) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    replaced_by_token_id UUID,
    device_info VARCHAR(500),
    ip_address VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_refresh_token_user
ON user_refresh_token(user_account_id);

CREATE INDEX IF NOT EXISTS idx_user_refresh_token_expires
ON user_refresh_token(expires_at);

CREATE INDEX IF NOT EXISTS idx_user_refresh_token_revoked
ON user_refresh_token(revoked_at);