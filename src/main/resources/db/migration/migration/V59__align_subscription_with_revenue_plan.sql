-- =========================================================
-- V59 - Align subscription table with current Subscription entity
-- =========================================================

-- =========================================================
-- 1. Add revenue plan relationship
-- =========================================================

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS plan_id UUID;

ALTER TABLE subscription
    ADD CONSTRAINT fk_subscription_plan
    FOREIGN KEY (plan_id)
    REFERENCES revenue_plan(id);


-- =========================================================
-- 2. Add commercial snapshot fields
-- =========================================================

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS plan_code_snapshot VARCHAR(80);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS plan_name_snapshot VARCHAR(150);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS amount_paid NUMERIC(12,2);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10);

ALTER TABLE subscription
    ADD COLUMN IF NOT EXISTS duration_days INTEGER;


-- =========================================================
-- 3. Existing subscription rows
--
-- There are currently ZERO rows in subscription, therefore
-- these columns can safely be made NOT NULL without data
-- migration/backfill.
-- =========================================================

ALTER TABLE subscription
    ALTER COLUMN plan_id SET NOT NULL;

ALTER TABLE subscription
    ALTER COLUMN plan_code_snapshot SET NOT NULL;

ALTER TABLE subscription
    ALTER COLUMN plan_name_snapshot SET NOT NULL;

ALTER TABLE subscription
    ALTER COLUMN amount_paid SET NOT NULL;

ALTER TABLE subscription
    ALTER COLUMN currency SET NOT NULL;


-- =========================================================
-- 4. Index for subscription -> revenue plan lookups
-- =========================================================

CREATE INDEX IF NOT EXISTS idx_subscription_plan_id
    ON subscription(plan_id);
