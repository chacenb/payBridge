package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.dto.ProviderCallbackResult;
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
 * Turns a raw operator callback body into a provider-agnostic {@link ProviderCallbackResult}.
 * Moov parsing is real, built against the captured shape in
 * {@code _MATERIALS/async callbacks/InitTrans_OnlineMerchantPayment.xml}. Airtel has no real
 * callback sample yet -- do not guess its payload shape; it needs the same kind of real
 * captured evidence Moov had before a real implementation can be written here.
 */
@Component
public class CallbackParserRegistry implements CallbackParser {

    @Override
    public ProviderCallbackResult parse(EPaymentOperator operator, String rawPayload) {
        return switch (operator) {
            case EPaymentOperator.MOOV_MONEY -> parseMoovCallback(rawPayload);
            case EPaymentOperator.AIRTEL_MONEY -> parseAirtelCallback(rawPayload);
        };
    }

    /* -------------------------------------------------------- */
    /* Moov callback parser */
    /* -------------------------------------------------------- */
    private ProviderCallbackResult parseMoovCallback(String rawPayload) {
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

    private UUID parseTransactionId(String originatorConversationId) {
        try {
            return UUID.fromString(originatorConversationId.trim());
        } catch (IllegalArgumentException ex) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                                      "Moov callback's OriginatorConversationID (\"" + originatorConversationId
                                      + "\") is not a paymentTransactionId -- MoovPaymentProvider is not yet sending it as one");
        }
    }

    private Document parseXml(String rawPayload) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(rawPayload)));
        } catch (Exception ex) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Malformed Moov callback XML: " + ex.getMessage());
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
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "Malformed Moov callback XML: " + ex.getMessage());
        }
    }

    /* -------------------------------------------------------- */
    /* Airtel callback parser */
    /* -------------------------------------------------------- */
    private ProviderCallbackResult parseAirtelCallback(String rawPayload) {
        throw new CustomException(EExceptionCode.DISABLED_FEATURE, "Airtel callback parsing is not implemented yet -- see AirtelCallbackParser's class javadoc");
    }
}
