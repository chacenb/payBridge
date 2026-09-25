package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.dto.ProviderCallbackResult;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.dto.ProviderSearchResult;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EProviderPaymentResultCode;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.UUID;

import static com.sahelys.payBridge.globals.constants.Globals.*;

/**
 * Every real XML parsing need in this codebase, in one flat class: both legs of a Moov
 * transaction-initiating call -- the async callback (real financial outcome, confirmed against
 * {@code _MATERIALS/async callbacks/InitTrans_OnlineMerchantPayment.xml}) and the sync ack
 * (immediate accept/reject, confirmed against {@code _MATERIALS/responses_prod/}) -- plus
 * {@code SearchTransactionByExtID}'s sync query result (reconciliation, not initiation; see
 * {@link #parseMoovSearchTransactionResult}). All three share the same XML/XPath mechanics
 * ({@link #parseXml}/{@link #evaluate}), which is exactly why they live together here rather
 * than as separate classes duplicating that boilerplate. Airtel has no real captured sample for
 * any leg yet -- do not guess its payload shape; it needs the same kind of real captured
 * evidence Moov had before a real implementation can be written here.
 */
@Component
public class ProviderXmlParser {

    /* -------------------------------------------------------- */
    /* Async callback parsing (the real financial outcome)       */
    /* -------------------------------------------------------- */

    public ProviderCallbackResult parseAsyncCallback(EPaymentOperator operator, String rawPayload) {
        return switch (operator) {
            case EPaymentOperator.MOOV_MONEY -> parseMoovAsyncCallback(rawPayload);
            case EPaymentOperator.AIRTEL_MONEY -> parseAirtelAsyncCallback(rawPayload);
        };
    }

    private ProviderCallbackResult parseMoovAsyncCallback(String rawPayload) {
        Document document = parseXml(rawPayload);
        XPath xpath = XPathFactory.newInstance().newXPath();

        String originatorConversationId = evaluate(xpath, document, MOMO_ORIGINATOR_CONVO_ID);
        String conversationId = evaluate(xpath, document, MOMO_CONVO_ID);
        String resultCode = evaluate(xpath, document, MOMO_RESULT_CODE);
        String resultDesc = evaluate(xpath, document, MOMO_RESULT_DESC);

        if (originatorConversationId.isBlank()) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Moov callback is missing OriginatorConversationID");
        if (resultCode.isBlank()) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Moov callback is missing ResultCode");

        UUID paymentTransactionId = parseTransactionId(originatorConversationId);
        EProviderPaymentResultCode providerPaymentResult = SUCCESS_CODE_0.equals(resultCode.trim())
                                                       ? EProviderPaymentResultCode.SUCCESS
                                                       : EProviderPaymentResultCode.FAILED;

        return ProviderCallbackResult.builder()
                                     .paymentTransactionId(paymentTransactionId)
                                     .providerPaymentTransactionId(conversationId.isBlank() ? null : conversationId.trim())
                                     .providerPaymentResultCode(providerPaymentResult)
                                     .message(resultDesc)
                                     .build();
    }

    private ProviderCallbackResult parseAirtelAsyncCallback(String rawPayload) {
        throw new CustomException(EExceptionCode.DISABLED_FEATURE, "Airtel callback parsing is not implemented yet -- no real captured sample exists");
    }

    private UUID parseTransactionId(String originatorConversationId) {
        try {
            return UUID.fromString(originatorConversationId.trim());
        } catch (IllegalArgumentException ex) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                                      "Moov callback's OriginatorConversationID (\"" + originatorConversationId
                                      + "\") is not a paymentTransactionId -- MoovPaymentProvider is not yet sending it as one");
        }
    }

    /* -------------------------------------------------------- */
    /* Sync ack parsing (accept/reject, never the real outcome)  */
    /* -------------------------------------------------------- */

    public ProviderPaymentResponse parseMoovSyncAck(String rawPayload) {
        Document document = parseXml(rawPayload);
        XPath xpath = XPathFactory.newInstance().newXPath();

        String conversationId = evaluate(xpath, document, MOMO_CONVO_ID);
        String responseCode = evaluate(xpath, document, MOMO_RESPONSE_CODE);
        String responseDesc = evaluate(xpath, document, MOMO_RESPONSE_DESC);

        if (responseCode.isBlank()) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Moov sync ack is missing ResponseCode");

        EProviderPaymentResultCode resultCode = SUCCESS_CODE_0.equals(responseCode.trim())
                                                ? EProviderPaymentResultCode.PENDING
                                                : EProviderPaymentResultCode.FAILED;

        return ProviderPaymentResponse.builder()
                                      .providerPaymentTransactionId(conversationId.isBlank() ? null : conversationId.trim())
                                      .resultCode(resultCode)
                                      .message(responseDesc)
                                      .build();
    }

    /* -------------------------------------------------------- */
    /* SearchTransactionByExtID (reconciliation, sync query)      */
    /* -------------------------------------------------------- */

    /**
     * The one real captured sample ({@code _MATERIALS/responses_prod/response-SearchTransactionByExtID-*.xml})
     * only ever shows a found-and-completed transaction: {@code ResultCode=0} +
     * {@code SearchTransactionByExtIDResult/BOCompletedTime}. There is no captured not-found or
     * found-but-failed sample, so unlike {@link #parseMoovAsyncCallback}/{@link #parseMoovSyncAck}
     * this deliberately does NOT resolve to {@link EProviderPaymentResultCode} -- there is no
     * evidence for what would distinguish a successful transaction from a failed one here, only
     * whether Moov found a match and when it completed. {@code found} is a best-effort read of
     * that one sample (0 + a populated completion time); revisit once a not-found/failed sample
     * exists instead of assuming this covers every case.
     */
    public ProviderSearchResult parseMoovSearchTransactionResult(String rawPayload) {
        Document document = parseXml(rawPayload);
        XPath xpath = XPathFactory.newInstance().newXPath();

        String resultCode = evaluate(xpath, document, MOMO_RESULT_CODE);
        String resultDesc = evaluate(xpath, document, MOMO_RESULT_DESC);
        String boCompletedTime = evaluate(xpath, document, MOMO_BO_COMPLETED_TIME);

        if (resultCode.isBlank()) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Moov search response is missing ResultCode");

        boolean found = SUCCESS_CODE_0.equals(resultCode.trim()) && !boCompletedTime.isBlank();

        return ProviderSearchResult.builder()
                                   .found(found)
                                   .completedAt(boCompletedTime.isBlank() ? null : boCompletedTime.trim())
                                   .message(resultDesc)
                                   .build();
    }

    /* -------------------------------------------------------- */
    /* Shared XML mechanics                                      */
    /* -------------------------------------------------------- */

    private Document parseXml(String rawPayload) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawPayload)));
        } catch (Exception ex) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Malformed Moov XML: " + ex.getMessage());
        }
    }

    /**
     * Namespace-agnostic by design -- matches on local name only, robust to prefix variance.
     */
    private String evaluate(XPath xpath, Document document, String localName) {
        try {
            String value = xpath.evaluate("//*[local-name()='" + localName + "']", document);
            return value == null ? "" : value;
        } catch (Exception ex) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Malformed Moov XML: " + ex.getMessage());
        }
    }
}
