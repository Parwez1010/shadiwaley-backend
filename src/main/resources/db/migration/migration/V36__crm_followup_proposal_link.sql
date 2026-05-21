ALTER TABLE crm_follow_up
ADD COLUMN IF NOT EXISTS proposal_id UUID;

ALTER TABLE crm_follow_up
ADD CONSTRAINT fk_crm_followup_proposal
FOREIGN KEY (proposal_id)
REFERENCES proposal(id);

CREATE INDEX IF NOT EXISTS idx_crm_followup_proposal
ON crm_follow_up(proposal_id);