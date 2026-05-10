CREATE TABLE crm_case (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL
        REFERENCES user_account(id) ON DELETE CASCADE,

    user_profile_id UUID NOT NULL
        REFERENCES user_profile(id) ON DELETE CASCADE,

    assigned_employee_id UUID
        REFERENCES employee_account(id),

    case_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(30) NOT NULL,

    source VARCHAR(50),
    summary VARCHAR(300),
    last_outcome VARCHAR(300),

    next_follow_up_at TIMESTAMPTZ,
    closed_at TIMESTAMPTZ,

    created_by_employee_id UUID
        REFERENCES employee_account(id),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_case_user
ON crm_case(user_account_id);

CREATE INDEX idx_crm_case_profile
ON crm_case(user_profile_id);

CREATE INDEX idx_crm_case_assigned_employee
ON crm_case(assigned_employee_id);

CREATE INDEX idx_crm_case_status_priority
ON crm_case(status, priority);

CREATE INDEX idx_crm_case_follow_up
ON crm_case(next_follow_up_at);

CREATE TABLE crm_case_note (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    crm_case_id UUID NOT NULL
        REFERENCES crm_case(id) ON DELETE CASCADE,

    employee_id UUID
        REFERENCES employee_account(id),

    note_type VARCHAR(50) NOT NULL,
    note TEXT NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_case_note_case
ON crm_case_note(crm_case_id, created_at DESC);

CREATE TABLE crm_follow_up (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    crm_case_id UUID NOT NULL
        REFERENCES crm_case(id) ON DELETE CASCADE,

    assigned_employee_id UUID
        REFERENCES employee_account(id),

    scheduled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    status VARCHAR(40) NOT NULL,
    channel VARCHAR(40) NOT NULL,

    purpose VARCHAR(200) NOT NULL,
    outcome VARCHAR(300),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crm_followup_case
ON crm_follow_up(crm_case_id);

CREATE INDEX idx_crm_followup_employee_status
ON crm_follow_up(assigned_employee_id, status);

CREATE INDEX idx_crm_followup_scheduled
ON crm_follow_up(scheduled_at);