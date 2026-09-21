CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_transaction_id UUID NOT NULL REFERENCES payment_transactions (id),
    provider VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider_payment_transaction_id VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_payment_attempts_status
        CHECK (status IN ('INITIATED', 'PROCESSING', 'SUCCESS', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_payment_attempts_transaction
    ON payment_attempts (payment_transaction_id);

-- Provider transaction ids are the correlation key for PM-07.1 (provider -> module callback).
CREATE UNIQUE INDEX uq_payment_attempts_provider_txn_id
    ON payment_attempts (provider_payment_transaction_id)
    WHERE provider_payment_transaction_id IS NOT NULL;
