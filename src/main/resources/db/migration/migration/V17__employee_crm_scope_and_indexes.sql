ALTER TABLE employee_account
ADD COLUMN IF NOT EXISTS assigned_district VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_employee_assigned_district
ON employee_account(assigned_district);

CREATE INDEX IF NOT EXISTS idx_parent_profile_district_state
ON parent_profile(district, state);

CREATE INDEX IF NOT EXISTS idx_user_profile_completion_status
ON user_profile(completion_pct, profile_status);