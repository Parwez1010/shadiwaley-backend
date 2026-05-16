ALTER TABLE crm_case
ADD COLUMN IF NOT EXISTS stage VARCHAR(60) NOT NULL DEFAULT 'CONTACT_PENDING';

ALTER TABLE crm_case
ADD COLUMN IF NOT EXISTS last_contact_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_crm_case_stage
ON crm_case(stage);

CREATE INDEX IF NOT EXISTS idx_crm_case_last_contact
ON crm_case(last_contact_at);