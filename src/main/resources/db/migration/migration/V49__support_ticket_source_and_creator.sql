ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS source VARCHAR(30);

ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS created_by_employee_id UUID;

UPDATE support_ticket
SET source = 'CUSTOMER'
WHERE source IS NULL;

ALTER TABLE support_ticket
    ALTER COLUMN source SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_support_ticket_source
    ON support_ticket(source);

CREATE INDEX IF NOT EXISTS idx_support_ticket_created_by_employee
    ON support_ticket(created_by_employee_id);