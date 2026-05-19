CREATE TABLE revenue_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',
    duration_days INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    features TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE family_subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    user_profile_id UUID NOT NULL REFERENCES user_profile(id),
    plan_id UUID NOT NULL REFERENCES revenue_plan(id),

    plan_code VARCHAR(80) NOT NULL,
    plan_name VARCHAR(150) NOT NULL,
    amount NUMERIC(12,2) NOT NULL DEFAULT 0,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',

    subscription_status VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,

    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,

    current_subscription BOOLEAN NOT NULL DEFAULT TRUE,

    assigned_by_employee_id UUID REFERENCES employee_account(id),
    assigned_by_name VARCHAR(150),

    source VARCHAR(80),
    note TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    subscription_id UUID NOT NULL REFERENCES family_subscription(id),
    user_account_id UUID NOT NULL REFERENCES user_account(id),
    user_profile_id UUID NOT NULL REFERENCES user_profile(id),

    plan_code VARCHAR(80) NOT NULL,
    plan_name VARCHAR(150) NOT NULL,

    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'INR',

    payment_mode VARCHAR(50) NOT NULL,
    payment_status VARCHAR(50) NOT NULL,

    payment_reference VARCHAR(200),
    payment_note TEXT,
    paid_at TIMESTAMPTZ,

    received_by_employee_id UUID REFERENCES employee_account(id),
    received_by_name VARCHAR(150),

    gateway_provider VARCHAR(80),
    gateway_order_id VARCHAR(200),
    gateway_payment_id VARCHAR(200),
    gateway_signature VARCHAR(500),
    gateway_refund_id VARCHAR(200),

    invoice_number VARCHAR(100),
    invoice_url VARCHAR(500),
    receipt_url VARCHAR(500),

    tax_amount NUMERIC(12,2),
    discount_amount NUMERIC(12,2),
    coupon_code VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_family_subscription_user ON family_subscription(user_account_id);
CREATE INDEX idx_family_subscription_profile ON family_subscription(user_profile_id);
CREATE INDEX idx_family_subscription_plan_code ON family_subscription(plan_code);
CREATE INDEX idx_family_subscription_status ON family_subscription(subscription_status);
CREATE INDEX idx_family_subscription_payment_status ON family_subscription(payment_status);
CREATE INDEX idx_family_subscription_start_at ON family_subscription(start_at);
CREATE INDEX idx_family_subscription_end_at ON family_subscription(end_at);
CREATE INDEX idx_family_subscription_created_at ON family_subscription(created_at);

CREATE INDEX idx_payment_user ON payment_transaction(user_account_id);
CREATE INDEX idx_payment_profile ON payment_transaction(user_profile_id);
CREATE INDEX idx_payment_subscription ON payment_transaction(subscription_id);
CREATE INDEX idx_payment_status ON payment_transaction(payment_status);
CREATE INDEX idx_payment_mode ON payment_transaction(payment_mode);
CREATE INDEX idx_payment_paid_at ON payment_transaction(paid_at);
CREATE INDEX idx_payment_created_at ON payment_transaction(created_at);
CREATE INDEX idx_payment_reference ON payment_transaction(payment_reference);

INSERT INTO revenue_plan (code, name, description, price, currency, duration_days, active, sort_order, features)
VALUES
('FREE_ONBOARDING', 'Free Onboarding', 'Basic profile creation and verification.', 0, 'INR', 0, TRUE, 1, 'Basic profile creation|Verification queue'),
('BASIC', 'Basic', 'Basic assisted matchmaking support.', 999, 'INR', 30, TRUE, 2, 'Basic assisted matching|Limited CRM support'),
('PREMIUM', 'Premium', 'Priority matching, CRM follow-ups and proposal assistance.', 2999, 'INR', 90, TRUE, 3, 'Priority matching|CRM follow-ups|Proposal assistance'),
('ELITE', 'Elite', 'High-touch assisted matchmaking and priority service.', 7999, 'INR', 180, TRUE, 4, 'High-touch matchmaking|Priority service|Dedicated support')
ON CONFLICT (code) DO NOTHING;