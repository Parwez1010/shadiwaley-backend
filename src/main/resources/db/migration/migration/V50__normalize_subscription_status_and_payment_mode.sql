UPDATE family_subscription
SET subscription_status = 'ACTIVE'
WHERE subscription_status = 'FREE';

UPDATE family_subscription
SET subscription_status = 'PAYMENT_PENDING'
WHERE subscription_status = 'PENDING_PAYMENT';

UPDATE family_subscription
SET subscription_status = 'CANCELLED'
WHERE subscription_status = 'REFUNDED';

UPDATE payment_transaction
SET payment_mode = 'RAZORPAY'
WHERE payment_mode = 'MANUAL_RAZORPAY'
AND gateway_provider = 'RAZORPAY';