ALTER TABLE user_feature_usage
ADD COLUMN IF NOT EXISTS usage_month VARCHAR(7);

UPDATE user_feature_usage
SET usage_month = TO_CHAR(usage_date, 'YYYY-MM')
WHERE usage_month IS NULL;

ALTER TABLE user_feature_usage
ALTER COLUMN usage_month SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_feature_usage_user_month
ON user_feature_usage(user_account_id, usage_month);