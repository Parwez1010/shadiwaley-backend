CREATE INDEX IF NOT EXISTS idx_payment_gateway_order
ON payment_transaction(gateway_order_id);

CREATE INDEX IF NOT EXISTS idx_payment_gateway_payment
ON payment_transaction(gateway_payment_id);