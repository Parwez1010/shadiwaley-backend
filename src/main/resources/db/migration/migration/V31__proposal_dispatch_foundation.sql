CREATE TABLE proposal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    from_profile_id UUID NOT NULL REFERENCES user_profile(id),
    to_profile_id UUID NOT NULL REFERENCES user_profile(id),

    crm_case_id UUID NULL REFERENCES crm_case(id),

    status VARCHAR(50) NOT NULL,
    dispatch_channel VARCHAR(50) NOT NULL,

    note TEXT,
    match_score INTEGER,
    share_profile_photo BOOLEAN NOT NULL DEFAULT FALSE,

    dispatched_by_employee_id UUID NULL REFERENCES employee_account(id),
    dispatched_by_name VARCHAR(150),
    dispatched_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    last_updated_by_employee_id UUID NULL REFERENCES employee_account(id),
    last_updated_by_name VARCHAR(150),
    last_updated_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_proposal_from_profile ON proposal(from_profile_id);
CREATE INDEX idx_proposal_to_profile ON proposal(to_profile_id);
CREATE INDEX idx_proposal_crm_case ON proposal(crm_case_id);
CREATE INDEX idx_proposal_status ON proposal(status);
CREATE INDEX idx_proposal_dispatched_at ON proposal(dispatched_at);

CREATE TABLE proposal_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    proposal_id UUID NOT NULL REFERENCES proposal(id) ON DELETE CASCADE,

    status VARCHAR(50) NOT NULL,
    note TEXT,

    actor_employee_id UUID NULL REFERENCES employee_account(id),
    actor_name VARCHAR(150),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_proposal_status_history_proposal
ON proposal_status_history(proposal_id, created_at DESC);

