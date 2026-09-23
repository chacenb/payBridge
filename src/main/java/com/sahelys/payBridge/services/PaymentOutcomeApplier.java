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
 * <p>Deliberately does NOT notify the client app here, even once terminal -- the payer
 * opening the paymentUrl isn't necessarily the same session that originated the request on
 * the client app (e.g. a shared link opened by someone else), so notifying/returning to the
 * client app is a separate, explicit, payer-triggered action -- see
 * {@link PaymentCallbackDeliveryService#notifyClientApp}.
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
