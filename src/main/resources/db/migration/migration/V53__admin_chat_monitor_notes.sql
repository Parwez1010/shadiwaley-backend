CREATE TABLE IF NOT EXISTS chat_room_internal_note (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    employee_id UUID NOT NULL,
    note VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_note_room
    ON chat_room_internal_note(room_id);

CREATE INDEX IF NOT EXISTS idx_chat_note_employee
    ON chat_room_internal_note(employee_id);

CREATE INDEX IF NOT EXISTS idx_chat_note_created_at
    ON chat_room_internal_note(created_at);