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
 * here, only surfaced alongside whatever the provider says.
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

    public ReconciliationOutcome reconcileWithProvider(UUID paymentTransactionId) {
        PaymentTransaction transaction = transactionService.findById(paymentTransactionId);

        if (transaction.getStatus().isTerminal()) {
            return new ReconciliationOutcome(transaction, false, null,
                    "Transaction is already " + transaction.getStatus() + " locally -- provider not queried.");
        }

        if (transaction.getProviderPaymentTransactionId() == null) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                    "PaymentTransaction " + paymentTransactionId + " has no providerPaymentTransactionId yet -- it was never submitted to a provider");
        }
        if (transaction.getPaymentOperator() != EPaymentOperator.MOOV_MONEY) {
            throw new CustomException(EExceptionCode.DISABLED_FEATURE,
                    "Provider reconciliation is only implemented for MOOV_MONEY currently");
        }

        // Matches the one real captured sample's own window: a single calendar day, the day
        // this transaction was created. Reconciling long after creation may need a wider
        // window -- not evidenced yet, so not guessed at here.
        var createdDate = transaction.getCreatedAt().toLocalDate();
        var startOfDay = createdDate.atStartOfDay();
        var endOfDay = createdDate.atTime(LocalTime.of(23, 59, 59));

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

        return new ReconciliationOutcome(transaction, result.isFound(), result.getCompletedAt(), result.getMessage());
    }

    public record ReconciliationOutcome(PaymentTransaction transaction, boolean providerFound,
                                         String providerCompletedAt, String providerMessage) {
    }
}
