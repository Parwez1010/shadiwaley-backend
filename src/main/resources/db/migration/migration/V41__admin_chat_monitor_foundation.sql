ALTER TABLE family_chat_room
ADD COLUMN IF NOT EXISTS proposal_id UUID REFERENCES proposal(id),
ADD COLUMN IF NOT EXISTS crm_case_id UUID REFERENCES crm_case(id),
ADD COLUMN IF NOT EXISTS assigned_employee_id UUID REFERENCES employee_account(id),
ADD COLUMN IF NOT EXISTS last_message_text VARCHAR(500),
ADD COLUMN IF NOT EXISTS last_message_type VARCHAR(30),
ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMPTZ,
ADD COLUMN IF NOT EXISTS last_message_by_name VARCHAR(150),
ADD COLUMN IF NOT EXISTS message_count BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS reported BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS needs_attention BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS last_report_reason VARCHAR(500);

CREATE TABLE IF NOT EXISTS chat_monitor_note (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES family_chat_room(id),
    note TEXT NOT NULL,
    created_by_employee_id UUID REFERENCES employee_account(id),
    created_by_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chat_family_decision_log (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL REFERENCES family_chat_room(id),
    decision VARCHAR(50) NOT NULL,
    note TEXT,
    next_follow_up_at TIMESTAMPTZ,
    created_by_employee_id UUID REFERENCES employee_account(id),
    created_by_name VARCHAR(150),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_last_message_at
ON family_chat_room(last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_assigned_last_message
ON family_chat_room(assigned_employee_id, last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_status_last_message
ON family_chat_room(status, last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_reported_last_message
ON family_chat_room(reported, last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_needs_attention_last_message
ON family_chat_room(needs_attention, last_message_at DESC);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_proposal
ON family_chat_room(proposal_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_room_crm_case
ON family_chat_room(crm_case_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_message_room_sent
ON family_chat_message(room_id, sent_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_monitor_note_room_created
ON chat_monitor_note(room_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_chat_family_decision_room_created
ON chat_family_decision_log(room_id, created_at DESC);