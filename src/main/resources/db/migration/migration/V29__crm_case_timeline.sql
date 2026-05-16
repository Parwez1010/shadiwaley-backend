CREATE TABLE IF NOT EXISTS crm_case_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    crm_case_id UUID NOT NULL
        REFERENCES crm_case(id) ON DELETE CASCADE,

    event_type VARCHAR(60) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,

    actor_employee_id UUID NULL
        REFERENCES employee_account(id),

    actor_name VARCHAR(150),

    old_value VARCHAR(300),
    new_value VARCHAR(300),

    metadata JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_crm_case_timeline_case_created
ON crm_case_timeline(crm_case_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_crm_case_timeline_event_type
ON crm_case_timeline(event_type);