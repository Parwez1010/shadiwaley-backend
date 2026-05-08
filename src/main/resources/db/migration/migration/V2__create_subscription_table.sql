CREATE TABLE subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    plan_type VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    activated_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    auto_renew BOOLEAN NOT NULL DEFAULT FALSE,
    razorpay_order_id VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscription_user_status ON subscription(user_account_id, status);
CREATE INDEX idx_subscription_plan_type ON subscription(plan_type);