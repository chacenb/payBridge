package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.builder.MoovMoneyXmlRequestBuilder;
import com.sahelys.payBridge.domain.dto.ProviderPaymentRequest;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.services.MoovMoneyWebClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Real MOOV_MONEY implementation of {@link #initiatePayment}: builds the real XML request
 * (with the caller's {@code paymentTransactionId} as {@code OriginatorConversationID}, so the
 * later async callback correlates automatically), sends it, and parses the sync ack.
 *
 * <p>The one remaining gap is {@link MoovMoneyWebClient#sendRequest} itself, which is still a
 * stub (always returns {@code null}) -- the network call to Huawei's real endpoint isn't
 * implemented yet. Until then, calling this against MOOV surfaces as a clean
 * {@code CustomException(RUNTINE_EXCEPTION)} (via {@code ProviderXmlParser.parseMoovSyncAck}
 * rejecting the null response, caught by {@code PaymentProviderSubmissionService}'s existing
 * "provider call failed" boundary), not a silent wrong result.
 */
@Component
@RequiredArgsConstructor
public class MoovPaymentProvider implements PaymentProvider {

    private final MoovMoneyXmlRequestBuilder xmlRequestBuilder;
    private final MoovMoneyWebClient         webClient;
    private final ProviderXmlParser          xmlParser;

    @Override
    public EPaymentOperator providerCode() {
        return EPaymentOperator.MOOV_MONEY;
    }

    @Override
    public ProviderPaymentResponse initiatePayment(ProviderPaymentRequest request) {
        String xmlRequest = xmlRequestBuilder.buildMerchantPaymentRequest(request.getPaymentTransactionId(), request.getCustomerPhone(), request.getAmount().toPlainString());
        String xmlResponse = webClient.sendRequest(EHuaweiEndpointType.ASYNC, xmlRequest);
        return xmlParser.parseMoovSyncAck(xmlResponse);
    }

    @Override
    public ProviderPaymentResponse checkPayment(String providerPaymentTransactionId) {
        return new ProviderPaymentResponse();
    }

    @Override public ProviderPaymentResponse giveChange() {
        return null;
    }
}
