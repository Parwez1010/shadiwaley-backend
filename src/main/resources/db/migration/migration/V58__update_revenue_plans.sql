-- =========================================================
-- Disable old plans
-- =========================================================

UPDATE revenue_plan
SET active = false,
    updated_at = NOW()
WHERE code IN (
    'BASIC',
    'PREMIUM',
    'ELITE',
    'BASIC_299',
    'PREMIUM_999',
    'ELITE_2499'
);


-- =========================================================
-- FREE ONBOARDING
-- =========================================================

UPDATE revenue_plan
SET
    name = 'Free Onboarding',
    description = 'Create your profile and complete onboarding.',
    price = 0.00,
    currency = 'INR',
    duration_days = NULL,
    billing_type = 'ONE_TIME',
    active = true,
    sort_order = 1,
    features =
        'Create family profile|Complete onboarding|Profile verification|Basic dashboard access',
    updated_at = NOW()
WHERE code = 'FREE_ONBOARDING';


-- =========================================================
-- 6 MONTH PLAN - ₹999
-- =========================================================

INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    billing_type,
    active,
    sort_order,
    features,

    rishta_requests_per_month,
    profiles_per_week,
    browse_profiles,
    family_chat,
    autopilot_dispatch,
    dedicated_crm,
    meeting_coordination,
    priority_profile_review,

    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'SIX_MONTH_999',
    '6 Months',
    'Assisted matchmaking for 6 months.',
    999.00,
    'INR',
    180,
    'ONE_TIME',
    true,
    2,
    'Unlimited profile browsing|5 rishta requests per month|Family chat after acceptance|CRM assistance',

    5,
    0,
    true,
    true,
    false,
    false,
    false,
    false,

    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM revenue_plan
    WHERE code = 'SIX_MONTH_999'
);


-- =========================================================
-- LIFETIME PLAN - ₹1999
-- =========================================================

INSERT INTO revenue_plan (
    id,
    code,
    name,
    description,
    price,
    currency,
    duration_days,
    billing_type,
    active,
    sort_order,
    features,

    rishta_requests_per_month,
    profiles_per_week,
    browse_profiles,
    family_chat,
    autopilot_dispatch,
    dedicated_crm,
    meeting_coordination,
    priority_profile_review,

    created_at,
    updated_at
)
SELECT
    gen_random_uuid(),
    'LIFETIME_1999',
    'Lifetime',
    'Lifetime assisted matchmaking with priority support.',
    1999.00,
    'INR',
    NULL,
    'ONE_TIME',
    true,
    3,
    'Unlimited profile browsing|Unlimited rishta requests|Family chat after acceptance|CRM assistance|Priority profile review|Meeting coordination',

    -1,
    -1,
    true,
    true,
    true,
    true,
    true,
    true,

    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM revenue_plan
    WHERE code = 'LIFETIME_1999'
);