package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.dto.ProviderCallbackResult;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.ECorrelationOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PM-07.1 Provider -> Payment Module. Takes an already-parsed {@link ProviderCallbackResult}
 * (the provider-specific parser -- e.g. Moov XML -- is separate, PM-06; the correlation
 * mechanism itself is confirmed against the real _MATERIALS captures, see
 * ProviderCallbackResult's javadoc) and applies it to the PaymentTransaction it references
 * by paymentTransactionId.
 *
 * <p>MVP simplification: correlates directly to PaymentTransaction -- no separate
 * PaymentAttempt layer (collapsed for the MVP). Assumes the callback always references a
 * known transaction, delivered exactly once, while that transaction is still non-terminal.
 * The commented-out blocks below handle the unknown-transaction, duplicate-delivery and
 * already-terminal cases; reinstate them once duplicate/inconsistent provider callbacks
 * need to be handled for real.
 */
@Service @RequiredArgsConstructor @Slf4j
public class PaymentCallbackCorrelationService {

    private final PaymentTransactionService transactionService;
    private final PaymentOutcomeApplier     outcomeApplier;

    @Transactional
    public ECorrelationOutcome correlate(ProviderCallbackResult result) {
        // --- Unknown-transaction handling, commented out for the MVP happy path ---
        // Optional<PaymentTransaction> transactionOpt = transactionService.tryFindById(result.getPaymentTransactionId());
        //
        // if (transactionOpt.isEmpty()) {
        //     log.warn("Callback for unknown paymentTransactionId {} -- retained, no transaction updated", result.getPaymentTransactionId());
        //     return ECorrelationOutcome.UNKNOWN_PROVIDER_TRANSACTION;
        // }
        //
        // PaymentTransaction transaction = transactionOpt.get();
        PaymentTransaction transaction = transactionService.findById(result.getPaymentTransactionId());

        if (result.getProviderPaymentTransactionId() != null && transaction.getProviderPaymentTransactionId() == null) {
            transaction = transactionService.attachProviderTransactionId(transaction.getId(), result.getProviderPaymentTransactionId());
        }

        // --- Already-applied / already-terminal handling, commented out for the MVP happy path ---
        // EPaymentTransactionStatusCode targetStatus = ProviderStatusMapper.toTransactionStatus(result.getProviderPaymentResultCode());
        //
        // if (transaction.getStatus() == targetStatus) {
        //     log.info("Duplicate callback for transaction {} -- already {}, ignored", transaction.getId(), transaction.getStatus());
        //     return ECorrelationOutcome.DUPLICATE_IGNORED;
        // }
        //
        // if (transaction.getStatus().isTerminal()) {
        //     log.warn("Callback for transaction {} claims {} but transaction is already terminal ({}) -- not overwritten", transaction.getId(), targetStatus, transaction.getStatus());
        //     return ECorrelationOutcome.DUPLICATE_IGNORED;
        // }

        outcomeApplier.apply(transaction, result.getProviderPaymentResultCode());
        return ECorrelationOutcome.APPLIED;
    }
}
