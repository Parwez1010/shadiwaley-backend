CREATE TABLE rishta_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    sender_user_id UUID NOT NULL
        REFERENCES user_account(id),

    receiver_user_id UUID NOT NULL
        REFERENCES user_account(id),

    sender_profile_id UUID NOT NULL
        REFERENCES user_profile(id),

    receiver_profile_id UUID NOT NULL
        REFERENCES user_profile(id),

    status VARCHAR(40) NOT NULL,

    sender_note VARCHAR(500),

    accepted_at TIMESTAMPTZ,
    rejected_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,

    chat_enabled BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rishta_sender
ON rishta_request(sender_user_id);

CREATE INDEX idx_rishta_receiver
ON rishta_request(receiver_user_id);

CREATE INDEX idx_rishta_status
ON rishta_request(status);

CREATE UNIQUE INDEX uq_active_rishta
ON rishta_request(sender_user_id, receiver_user_id)
WHERE status IN ('PENDING', 'ACCEPTED');