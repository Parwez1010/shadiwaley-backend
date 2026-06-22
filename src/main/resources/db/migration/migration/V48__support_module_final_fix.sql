ALTER TABLE support_ticket_reply
    ADD COLUMN IF NOT EXISTS sender_type VARCHAR(30);

UPDATE support_ticket_reply
SET sender_type = 'CUSTOMER'
WHERE sender_type IS NULL;

ALTER TABLE support_ticket_reply
    ALTER COLUMN sender_type SET NOT NULL;