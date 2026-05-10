ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS media_file_id UUID REFERENCES media_file(id);

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(30) DEFAULT 'VISIBLE';

ALTER TABLE family_chat_room
ADD COLUMN IF NOT EXISTS blocked BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE family_chat_room
ADD COLUMN IF NOT EXISTS blocked_by_user_id UUID REFERENCES user_account(id);

ALTER TABLE family_chat_room
ADD COLUMN IF NOT EXISTS closed_at TIMESTAMPTZ;

CREATE TABLE IF NOT EXISTS chat_message_report (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    message_id UUID NOT NULL
        REFERENCES family_chat_message(id) ON DELETE CASCADE,

    reporter_user_id UUID NOT NULL
        REFERENCES user_account(id),

    reason VARCHAR(100) NOT NULL,

    details VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_chat_report_message
ON chat_message_report(message_id);

CREATE INDEX idx_chat_report_user
ON chat_message_report(reporter_user_id);