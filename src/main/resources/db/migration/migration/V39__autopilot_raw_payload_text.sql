ALTER TABLE autopilot_dispatch_batch
ALTER COLUMN raw_webhook_payload TYPE TEXT
USING raw_webhook_payload::TEXT;

ALTER TABLE autopilot_dispatch_item
ALTER COLUMN raw_webhook_payload TYPE TEXT
USING raw_webhook_payload::TEXT;