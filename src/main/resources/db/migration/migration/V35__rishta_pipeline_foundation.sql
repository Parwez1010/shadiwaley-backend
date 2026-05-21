CREATE TABLE IF NOT EXISTS rishta_pipeline_note (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    proposal_id UUID NOT NULL
        REFERENCES proposal(id) ON DELETE CASCADE,

    note TEXT NOT NULL,

    created_by_employee_id UUID
        REFERENCES employee_account(id),

    created_by_name VARCHAR(150),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rishta_pipeline_note_proposal
ON rishta_pipeline_note(proposal_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_proposal_status
ON proposal(status);

CREATE INDEX IF NOT EXISTS idx_proposal_created_at
ON proposal(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_proposal_updated_at
ON proposal(updated_at DESC);

CREATE INDEX IF NOT EXISTS idx_proposal_dispatched_at
ON proposal(dispatched_at DESC);

CREATE INDEX IF NOT EXISTS idx_proposal_from_profile
ON proposal(from_profile_id);

CREATE INDEX IF NOT EXISTS idx_proposal_to_profile
ON proposal(to_profile_id);

CREATE INDEX IF NOT EXISTS idx_proposal_crm_case
ON proposal(crm_case_id);

CREATE INDEX IF NOT EXISTS idx_crm_followup_scheduled_status
ON crm_follow_up(scheduled_at, status);

CREATE INDEX IF NOT EXISTS idx_crm_followup_assigned_employee
ON crm_follow_up(assigned_employee_id);