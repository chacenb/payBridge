CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    client_app_id VARCHAR(64) NOT NULL,
    client_payment_request_id VARCHAR(128) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(512),
    callback_url VARCHAR(2048) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_transactions_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED', 'EXPIRED'))
);

-- Idempotency identity per architecture-reference section 24 (frozen ahead of the
-- dedicated idempotency step so the constraint ships with the table, not bolted on later).
CREATE UNIQUE INDEX uq_payment_transactions_client_request
    ON payment_transactions (client_app_id, client_payment_request_id);

CREATE INDEX idx_payment_transactions_status
    ON payment_transactions (status);
