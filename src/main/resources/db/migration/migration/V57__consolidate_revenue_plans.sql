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