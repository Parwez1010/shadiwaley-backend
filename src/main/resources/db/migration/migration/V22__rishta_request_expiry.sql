ALTER TABLE rishta_request
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMPTZ;

UPDATE rishta_request
SET expires_at = created_at + INTERVAL '15 days'
WHERE expires_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_rishta_request_expiry
ON rishta_request(status, expires_at);