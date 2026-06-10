ALTER TABLE family_chat_room
ADD COLUMN IF NOT EXISTS chat_mode VARCHAR(40) NOT NULL DEFAULT 'DIRECT_FAMILY';

CREATE INDEX IF NOT EXISTS idx_family_chat_room_chat_mode
ON family_chat_room(chat_mode);