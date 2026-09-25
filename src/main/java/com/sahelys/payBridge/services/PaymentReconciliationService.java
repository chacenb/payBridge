package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.builder.MoovMoneyXmlRequestBuilder;
import com.sahelys.payBridge.domain.dto.ProviderSearchResult;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.provider.ProviderXmlParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * {@code SearchTransactionByExtID}-backed reconciliation -- a sync query against Moov for a
 * transaction's real state, for when no async callback arrived. Deliberately NOT built on
 * {@link com.sahelys.payBridge.provider.PaymentProvider}: that interface's own javadoc says
 * plain sync query commands like this one "do not belong behind this interface -- they are
 * reconciliation/query concerns, not payment initiation." And unlike
 * {@link PaymentProviderSubmissionService}/{@link PaymentCallbackCorrelationService}, this
 * deliberately does NOT call {@link PaymentOutcomeApplier} either: the real captured response
 * for this command carries no success/failure signal (see {@link ProviderXmlParser#parseMoovSearchTransactionResult}),
 * so there is nothing confident to apply -- the local transaction's status is never mutated
 * here, the raw provider fields are just relayed alongside it.
 *
 * <p>Moov only, same as everywhere else in this codebase that hasn't had real Airtel evidence
 * yet -- rejects outright rather than guessing at an Airtel equivalent.
 */
@Service
@RequiredArgsConstructor
public class PaymentReconciliationService {

    private final PaymentTransactionService    transactionService;
    private final MoovMoneyXmlRequestBuilder   xmlRequestBuilder;
    private final MoovMoneyWebClient           webClient;
    private final ProviderXmlParser            xmlParser;

    /**
     * {@code startDate}/{@code endDate} are supplied by the caller (the admin dashboard) rather
     * than derived here -- the search window is the admin's call to make, e.g. widening it if
     * the transaction's exact processing day on Moov's side is uncertain.
     */
    public ReconciliationOutcome reconcileWithProvider(UUID paymentTransactionId, LocalDate startDate, LocalDate endDate) {
        PaymentTransaction transaction = transactionService.findById(paymentTransactionId);

        if (transaction.getStatus().isTerminal()) {
            return new ReconciliationOutcome(transaction, false, null, null, null);
        }

        if (transaction.getProviderPaymentTransactionId() == null) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                    "PaymentTransaction " + paymentTransactionId + " has no providerPaymentTransactionId yet -- it was never submitted to a provider");
        }
        if (transaction.getPaymentOperator() != EPaymentOperator.MOOV_MONEY) {
            throw new CustomException(EExceptionCode.DISABLED_FEATURE,
                    "Provider reconciliation is only implemented for MOOV_MONEY currently");
        }

        LocalDateTime startOfDay = startDate.atStartOfDay();
        LocalDateTime endOfDay = endDate.atTime(LocalTime.of(23, 59, 59));

        String xmlRequest = xmlRequestBuilder.buildSearchTransactionByExtIdRequest(
                transaction.getProviderPaymentTransactionId(), startOfDay, endOfDay);

        ProviderSearchResult result;
        try {
            String xmlResponse = webClient.sendRequest(EHuaweiEndpointType.SYNC, xmlRequest);
            result = xmlParser.parseMoovSearchTransactionResult(xmlResponse);
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException(EExceptionCode.RUNTINE_EXCEPTION, ex);
        }

        return new ReconciliationOutcome(transaction, true, result.getResultCode(), result.getResultDesc(), result.getCompletedAt());
    }

    /**
     * {@code queriedProvider} is a fact about what this call did (skipped when already terminal),
     * never a guess about the provider's data. The three {@code provider*} fields are the raw,
     * unmodified fields {@link ProviderXmlParser#parseMoovSearchTransactionResult} returned --
     * null (not defaulted/interpreted) whenever {@code queriedProvider} is false.
     */
    public record ReconciliationOutcome(PaymentTransaction transaction, boolean queriedProvider,
                                         String providerResultCode, String providerResultDesc,
                                         String providerCompletedAt) {
    }
}
