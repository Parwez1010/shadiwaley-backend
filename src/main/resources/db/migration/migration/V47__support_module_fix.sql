ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS last_message VARCHAR(500);

ALTER TABLE support_ticket
    ADD COLUMN IF NOT EXISTS last_replied_at TIMESTAMP;

ALTER TABLE support_ticket
    RENAME COLUMN user_account_id TO customer_user_id;