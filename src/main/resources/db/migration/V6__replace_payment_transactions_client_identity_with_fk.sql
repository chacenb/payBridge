-- payment_transactions is empty in every environment as of this migration (confirmed local
-- and preprod) -- a direct column swap is safe with no backfill needed.
ALTER TABLE payment_transactions
    ADD COLUMN client_payment_request_ref UUID NOT NULL
        REFERENCES client_payment_requests(id),
    DROP COLUMN client_app_id,
    DROP COLUMN client_payment_request_id;

DROP INDEX IF EXISTS uq_payment_transactions_client_request;
