ALTER TABLE family_chat_room
    ADD COLUMN IF NOT EXISTS from_user_id UUID;

ALTER TABLE family_chat_room
    ADD COLUMN IF NOT EXISTS to_user_id UUID;

ALTER TABLE family_chat_room
    ADD COLUMN IF NOT EXISTS from_profile_id UUID;

ALTER TABLE family_chat_room
    ADD COLUMN IF NOT EXISTS to_profile_id UUID;

CREATE INDEX IF NOT EXISTS idx_family_chat_room_from_user
    ON family_chat_room(from_user_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_to_user
    ON family_chat_room(to_user_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_from_profile
    ON family_chat_room(from_profile_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_to_profile
    ON family_chat_room(to_profile_id);