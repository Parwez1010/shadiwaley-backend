CREATE TABLE audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    actor_id UUID,
    actor_type VARCHAR(40),
    actor_name VARCHAR(150),
    actor_role VARCHAR(60),

    action VARCHAR(80) NOT NULL,

    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,

    description VARCHAR(500),

    metadata TEXT,

    ip_address VARCHAR(80),
    user_agent VARCHAR(500),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor
ON audit_log(actor_id, created_at DESC);

CREATE INDEX idx_audit_entity
ON audit_log(entity_type, entity_id, created_at DESC);

CREATE INDEX idx_audit_action
ON audit_log(action, created_at DESC);

CREATE INDEX idx_audit_created
ON audit_log(created_at DESC);