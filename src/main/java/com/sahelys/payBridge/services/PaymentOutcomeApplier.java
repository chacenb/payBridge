package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.domain.enums.EProviderPaymentResultCode;
import com.sahelys.payBridge.provider.ProviderStatusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The one place a provider result (whichever leg it came from -- the synchronous ack inside
 * PaymentProviderSubmissionService, or the later async callback inside
 * PaymentCallbackCorrelationService) is turned into a PaymentTransaction transition. Kept in
 * one place so the two legs cannot drift apart.
 *
 * <p>Does not notify the client app itself: reaching a terminal status publishes an event
 * from {@link PaymentTransactionService#changeTransactionStatusTo}, delivered after commit by
 * {@link PaymentCallbackDeliveryService#onPaymentTransactionTerminated}.
 *
 * <p>MVP simplification: operates on PaymentTransaction directly -- no separate
 * PaymentAttempt layer (collapsed for the MVP).
 */
@Service
@RequiredArgsConstructor
public class PaymentOutcomeApplier {

    private final PaymentTransactionService transactionService;

    /**
     * Idempotent on repeat calls with the same status: a transition already at its target
     * is skipped rather than re-applied (avoids the illegal self-transition every status
     * enum forbids).
     */
    @Transactional
    public PaymentTransaction applyProviderCallbackResultCodeToLocalTransaction(PaymentTransaction transaction, EProviderPaymentResultCode providerPaymentResultCode) {
        EPaymentTransactionStatusCode targetStatus = ProviderStatusMapper.toLocalTransactionStatus(providerPaymentResultCode);
        if (transaction.getStatus() != targetStatus) transaction = transactionService.changeTransactionStatusTo(transaction.getId(), targetStatus);
        return transaction;
    }
}
