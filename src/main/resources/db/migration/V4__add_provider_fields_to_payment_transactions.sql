-- MVP simplification: PaymentAttempt is collapsed into PaymentTransaction (no separate
-- per-provider-try entity for now). Both columns are nullable: provider is unknown until
-- selected, providerPaymentTransactionId is unknown until the provider acknowledges.
-- payment_attempts (V3) is left in place, unused, rather than dropped.
ALTER TABLE payment_transactions
    ADD COLUMN provider VARCHAR(32),
    ADD COLUMN provider_payment_transaction_id VARCHAR(128);
