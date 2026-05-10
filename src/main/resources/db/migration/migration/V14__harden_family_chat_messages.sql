ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS reply_to_message_id UUID REFERENCES family_chat_message(id);

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ;

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS deleted_by_user_id UUID REFERENCES user_account(id);

CREATE INDEX IF NOT EXISTS idx_chat_message_room_cursor
ON family_chat_message(room_id, sent_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_chat_message_room_unread
ON family_chat_message(room_id, sender_user_id, read_at);