CREATE TABLE family_chat_room (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    rishta_request_id UUID NOT NULL UNIQUE
        REFERENCES rishta_request(id) ON DELETE CASCADE,

    boy_user_id UUID NOT NULL
        REFERENCES user_account(id),

    girl_user_id UUID NOT NULL
        REFERENCES user_account(id),

    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_room_boy_user
ON family_chat_room(boy_user_id);

CREATE INDEX idx_chat_room_girl_user
ON family_chat_room(girl_user_id);

CREATE TABLE family_chat_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    room_id UUID NOT NULL
        REFERENCES family_chat_room(id) ON DELETE CASCADE,

    sender_user_id UUID NOT NULL
        REFERENCES user_account(id),

    message_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',

    content TEXT NOT NULL,

    delivery_status VARCHAR(30) NOT NULL DEFAULT 'SENT',

    sent_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    edited_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_chat_message_room_sent
ON family_chat_message(room_id, sent_at ASC);

CREATE INDEX idx_chat_message_sender
ON family_chat_message(sender_user_id);