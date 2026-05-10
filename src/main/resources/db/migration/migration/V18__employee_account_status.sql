ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS account_status VARCHAR(30);

UPDATE employee_account
SET account_status = 'ACTIVE'
WHERE account_status IS NULL;

ALTER TABLE employee_account
ALTER COLUMN account_status SET NOT NULL;