CREATE TABLE media_file (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    user_profile_id UUID NOT NULL REFERENCES user_profile(id) ON DELETE CASCADE,

    media_type VARCHAR(40) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL UNIQUE,
    content_type VARCHAR(100) NOT NULL,
    file_size_bytes BIGINT NOT NULL,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    visibility VARCHAR(50) NOT NULL,
    whatsapp_consent VARCHAR(40),
    review_status VARCHAR(40) NOT NULL DEFAULT 'PENDING_REVIEW',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_media_user_account ON media_file(user_account_id);
CREATE INDEX idx_media_user_profile ON media_file(user_profile_id);
CREATE INDEX idx_media_type ON media_file(media_type);
CREATE INDEX idx_media_review_status ON media_file(review_status);