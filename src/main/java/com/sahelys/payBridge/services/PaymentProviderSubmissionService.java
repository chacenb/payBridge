package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.dto.ProviderPaymentRequest;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.provider.PaymentProvider;
import com.sahelys.payBridge.provider.ProviderMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * The single, frozen entry point for submitting a PaymentTransaction to a provider. Whoever
 * builds PM-04 (Payment UI, after the user picks a provider) or a future auto-dispatch calls
 * this -- and only this -- rather than each inventing their own "call provider, applyProviderCallbackResultCodeToLocalTransaction the
 * sync-ack outcome" glue. That glue is exactly what tends to drift inconsistently across a
 * codebase once more than one person is building against it.
 *
 * <p>MVP simplification: operates on PaymentTransaction directly -- no separate
 * PaymentAttempt layer (collapsed for the MVP), so there's no "another attempt already in
 * flight" check anymore either: the transaction's own non-terminal status is the only guard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProviderSubmissionService {

    private final PaymentTransactionService transactionService;
    private final PaymentOutcomeApplier     outcomeApplier;
    private final ProviderMatcher           providerMatcher;

    /**
     * Calls the provider's synchronous leg for {@code paymentTransactionId}. Per the real
     * Moov captures, that leg can only ever resolve to "accepted for processing" or
     * "rejected" -- never the real financial outcome, which arrives later through
     * {@link PaymentCallbackCorrelationService}. If the provider call itself fails
     * (network/timeout), the outcome is genuinely unknown: the transaction is left as-is for
     * reconciliation rather than guessed at as FAILED.
     *
     * @throws CustomException if the transaction is already terminal or the provider call
     *                         itself failed.
     */
    @Transactional
    public PaymentTransaction submitToProvider(UUID paymentTransactionId, String providerCode, String customerPhone) {
        PaymentTransaction transaction = transactionService.findById(paymentTransactionId);

        if (transaction.getStatus().isTerminal()) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "PaymentTransaction " + paymentTransactionId + " is already " + transaction.getStatus());

        EPaymentOperator operator = parseOperator(providerCode);
        PaymentProvider paymentProvider = providerMatcher.getProviderFromOperator(operator);
        transaction = transactionService.attachPaymentOperatorToTransaction(paymentTransactionId, operator);

        if (transaction.getStatus() != EPaymentTransactionStatusCode.PROCESSING) transaction = transactionService.changeTransactionStatusTo(paymentTransactionId, EPaymentTransactionStatusCode.PROCESSING);

        ProviderPaymentRequest request = ProviderPaymentRequest.builder()
                                                               .paymentTransactionId(paymentTransactionId)
                                                               .amount(transaction.getAmount())
                                                               .currency(transaction.getCurrency())
                                                               .customerPhone(customerPhone)
                                                               .build();

        ProviderPaymentResponse response;
        try {
            response = paymentProvider.initiatePayment(request);
        } catch (Exception ex) {
            log.error("Provider {} initiatePayment threw for transaction {} -- outcome is unknown, transaction left as-is for reconciliation rather than assumed FAILED", providerCode, paymentTransactionId, ex);
            throw new CustomException(EExceptionCode.RUNTINE_EXCEPTION, ex);
        }

        if (response.getProviderPaymentTransactionId() != null) {
            transaction = transactionService.attachProviderTransactionIdToLocalTransaction(paymentTransactionId, response.getProviderPaymentTransactionId());
        }

        return outcomeApplier.applyProviderCallbackResultCodeToLocalTransaction(transaction, response.getResultCode());
    }

    /**
     * A code that isn't MOOV or AIRTEL at all is a caller bug (ENUM_MISMATCH). Parsed once
     * here and reused for both provider resolution and what gets persisted on the
     * transaction, so the stored value is always the canonical enum name -- never whatever
     * casing/spelling the caller happened to send.
     */
    private EPaymentOperator parseOperator(String providerCode) {
        try {
            return EPaymentOperator.valueOf(providerCode.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new CustomException(EExceptionCode.ENUM_MISMATCH, "\"" + providerCode + "\" is not a recognized mobile money operator");
        }
    }
}
