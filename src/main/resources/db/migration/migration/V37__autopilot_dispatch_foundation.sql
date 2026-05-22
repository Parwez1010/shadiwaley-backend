CREATE TABLE autopilot_dispatch_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_account_id UUID NOT NULL REFERENCES user_account(id),
    user_profile_id UUID NOT NULL REFERENCES user_profile(id),
    crm_case_id UUID REFERENCES crm_case(id),

    assigned_employee_id UUID REFERENCES employee_account(id),
    assigned_employee_name VARCHAR(150),

    queue_status VARCHAR(50) NOT NULL,
    priority_score INTEGER NOT NULL DEFAULT 0,
    reason VARCHAR(300),
    blocked_reason VARCHAR(300),

    plan_code VARCHAR(80),
    plan_name VARCHAR(150),
    payment_status VARCHAR(50),
    subscription_status VARCHAR(50),

    last_dispatch_at TIMESTAMPTZ,
    next_dispatch_due_at TIMESTAMPTZ,
    dispatch_count INTEGER NOT NULL DEFAULT 0,
    pending_response_count INTEGER NOT NULL DEFAULT 0,

    provider_name VARCHAR(100),
    automation_run_id VARCHAR(150),
    ai_suggested BOOLEAN NOT NULL DEFAULT FALSE,
    ai_confidence NUMERIC(5,2),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE autopilot_dispatch_draft (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    queue_id UUID NOT NULL REFERENCES autopilot_dispatch_queue(id) ON DELETE CASCADE,
    source_profile_id UUID NOT NULL REFERENCES user_profile(id),

    channel VARCHAR(50) NOT NULL,
    share_profile_photo BOOLEAN NOT NULL DEFAULT TRUE,
    note TEXT,

    message_preview TEXT,
    warnings TEXT,
    can_send BOOLEAN NOT NULL DEFAULT TRUE,
    block_reason VARCHAR(300),

    created_by_employee_id UUID REFERENCES employee_account(id),
    created_by_name VARCHAR(150),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

CREATE TABLE autopilot_dispatch_batch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    queue_id UUID NOT NULL REFERENCES autopilot_dispatch_queue(id),
    source_profile_id UUID NOT NULL REFERENCES user_profile(id),

    status VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    send_mode VARCHAR(50),

    share_profile_photo BOOLEAN NOT NULL DEFAULT TRUE,
    note TEXT,

    items_count INTEGER NOT NULL DEFAULT 0,

    sent_by_employee_id UUID REFERENCES employee_account(id),
    sent_by_name VARCHAR(150),
    sent_at TIMESTAMPTZ,

    provider_name VARCHAR(100),
    provider_message_id VARCHAR(200),
    whatsapp_template_id VARCHAR(200),
    delivery_status VARCHAR(80),
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    replied_at TIMESTAMPTZ,
    raw_webhook_payload JSONB,

    automation_run_id VARCHAR(150),
    ai_suggested BOOLEAN NOT NULL DEFAULT FALSE,
    ai_confidence NUMERIC(5,2),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE autopilot_dispatch_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    dispatch_batch_id UUID NOT NULL REFERENCES autopilot_dispatch_batch(id) ON DELETE CASCADE,
    queue_id UUID NOT NULL REFERENCES autopilot_dispatch_queue(id),
    source_profile_id UUID NOT NULL REFERENCES user_profile(id),
    candidate_profile_id UUID NOT NULL REFERENCES user_profile(id),

    proposal_id UUID REFERENCES proposal(id),

    status VARCHAR(50) NOT NULL,
    response_status VARCHAR(50) NOT NULL,

    match_score INTEGER,
    compatibility_score INTEGER,
    dispatch_readiness VARCHAR(80),
    block_reason VARCHAR(300),

    photo_included BOOLEAN NOT NULL DEFAULT FALSE,
    photo_blocked_reason VARCHAR(300),

    response_note TEXT,
    responded_at TIMESTAMPTZ,

    provider_name VARCHAR(100),
    provider_message_id VARCHAR(200),
    delivery_status VARCHAR(80),
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    replied_at TIMESTAMPTZ,
    raw_webhook_payload JSONB,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_autopilot_queue_user ON autopilot_dispatch_queue(user_account_id);
CREATE INDEX idx_autopilot_queue_profile ON autopilot_dispatch_queue(user_profile_id);
CREATE INDEX idx_autopilot_queue_case ON autopilot_dispatch_queue(crm_case_id);
CREATE INDEX idx_autopilot_queue_employee ON autopilot_dispatch_queue(assigned_employee_id);
CREATE INDEX idx_autopilot_queue_status ON autopilot_dispatch_queue(queue_status);
CREATE INDEX idx_autopilot_queue_due ON autopilot_dispatch_queue(next_dispatch_due_at);
CREATE INDEX idx_autopilot_queue_priority ON autopilot_dispatch_queue(priority_score DESC);
CREATE INDEX idx_autopilot_queue_plan ON autopilot_dispatch_queue(plan_code);
CREATE INDEX idx_autopilot_queue_payment ON autopilot_dispatch_queue(payment_status);
CREATE INDEX idx_autopilot_queue_subscription ON autopilot_dispatch_queue(subscription_status);

CREATE INDEX idx_autopilot_draft_queue ON autopilot_dispatch_draft(queue_id);
CREATE INDEX idx_autopilot_draft_source ON autopilot_dispatch_draft(source_profile_id);

CREATE INDEX idx_autopilot_batch_queue ON autopilot_dispatch_batch(queue_id);
CREATE INDEX idx_autopilot_batch_source ON autopilot_dispatch_batch(source_profile_id);
CREATE INDEX idx_autopilot_batch_status ON autopilot_dispatch_batch(status);
CREATE INDEX idx_autopilot_batch_channel ON autopilot_dispatch_batch(channel);
CREATE INDEX idx_autopilot_batch_sent_at ON autopilot_dispatch_batch(sent_at);
CREATE INDEX idx_autopilot_batch_created_at ON autopilot_dispatch_batch(created_at);

CREATE INDEX idx_autopilot_item_batch ON autopilot_dispatch_item(dispatch_batch_id);
CREATE INDEX idx_autopilot_item_queue ON autopilot_dispatch_item(queue_id);
CREATE INDEX idx_autopilot_item_source ON autopilot_dispatch_item(source_profile_id);
CREATE INDEX idx_autopilot_item_candidate ON autopilot_dispatch_item(candidate_profile_id);
CREATE INDEX idx_autopilot_item_proposal ON autopilot_dispatch_item(proposal_id);
CREATE INDEX idx_autopilot_item_status ON autopilot_dispatch_item(status);
CREATE INDEX idx_autopilot_item_response ON autopilot_dispatch_item(response_status);

CREATE UNIQUE INDEX uq_autopilot_item_batch_candidate
ON autopilot_dispatch_item(dispatch_batch_id, candidate_profile_id);

CREATE INDEX idx_autopilot_item_source_candidate_status
ON autopilot_dispatch_item(source_profile_id, candidate_profile_id, status);