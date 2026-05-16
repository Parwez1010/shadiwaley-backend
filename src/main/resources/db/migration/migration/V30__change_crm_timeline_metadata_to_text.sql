ALTER TABLE crm_case_timeline
ALTER COLUMN metadata TYPE TEXT
USING metadata::text;