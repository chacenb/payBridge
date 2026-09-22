CREATE TABLE client_payment_requests (
    id UUID PRIMARY KEY,
    client_app_id VARCHAR(64) NOT NULL,
    client_payment_request_id VARCHAR(128) NOT NULL,
    amount NUMERIC(18,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(512),
    callback_url VARCHAR(2048) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Idempotency identity per architecture-reference section 24 -- this is the client's own
-- identity, so the constraint belongs here, not on payment_transactions.
CREATE UNIQUE INDEX uq_client_payment_requests_client_request
    ON client_payment_requests (client_app_id, client_payment_request_id);
