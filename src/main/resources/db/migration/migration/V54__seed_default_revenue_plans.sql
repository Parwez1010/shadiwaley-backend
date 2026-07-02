INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    active,
    sort_order,
    features,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'FREE_ONBOARDING',
    'Free Onboarding',
    'Default free onboarding plan',
    0.00,
    'INR',
    30,
    true,
    1,
    'Create family profile|Complete onboarding|Upload profile documents|Basic dashboard access',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM revenue_plan WHERE code = 'FREE_ONBOARDING'
);

INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    active,
    sort_order,
    features,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'BASIC_299',
    'Basic ₹299',
    'Basic monthly plan',
    299.00,
    'INR',
    30,
    true,
    2,
    'Unlimited profile browsing|5 rishta requests per month|Family chat after acceptance',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM revenue_plan WHERE code = 'BASIC_299'
);

INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    active,
    sort_order,
    features,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'PREMIUM_999',
    'Premium ₹999',
    'Premium monthly plan',
    999.00,
    'INR',
    30,
    true,
    3,
    'Unlimited rishta requests|CRM assistance|Autopilot dispatch|Meeting coordination',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM revenue_plan WHERE code = 'PREMIUM_999'
);

INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    active,
    sort_order,
    features,
    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'ELITE_2499',
    'Elite ₹2499',
    'Elite premium plan',
    2499.00,
    'INR',
    0,
    true,
    4,
    'Unlimited profile browsing|Senior CRM support|Priority review|Full meeting coordination',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM revenue_plan WHERE code = 'ELITE_2499'
);