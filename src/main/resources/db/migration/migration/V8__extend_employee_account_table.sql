ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS phone VARCHAR(15);

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS assigned_district VARCHAR(100);

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS base_salary INTEGER NOT NULL DEFAULT 0;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS incentive_per_dispatch INTEGER NOT NULL DEFAULT 500;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS incentive_per_engagement INTEGER NOT NULL DEFAULT 2000;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS joining_date DATE;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS emergency_contact VARCHAR(200);

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS notes VARCHAR(500);

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS must_change_password BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER NOT NULL DEFAULT 0;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS locked_until TIMESTAMPTZ;

ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_employee_assigned_district
ON employee_account(assigned_district);