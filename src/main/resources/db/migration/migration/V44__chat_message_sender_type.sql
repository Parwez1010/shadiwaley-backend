ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS sender_type VARCHAR(30) NOT NULL DEFAULT 'CUSTOMER';

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS sender_employee_id UUID REFERENCES employee_account(id);

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS assisted_user_id UUID REFERENCES user_account(id);

ALTER TABLE family_chat_message
ADD COLUMN IF NOT EXISTS assisted_family_name VARCHAR(150);

CREATE INDEX IF NOT EXISTS idx_family_chat_message_sender_employee
ON family_chat_message(sender_employee_id);

CREATE INDEX IF NOT EXISTS idx_family_chat_message_assisted_user
ON family_chat_message(assisted_user_id);