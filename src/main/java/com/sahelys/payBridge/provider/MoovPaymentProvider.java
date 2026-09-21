package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.dto.ProviderPaymentRequest;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import org.springframework.stereotype.Component;

/**
 * Bare skeleton: registers MOOV as a real, callable operator so
 * PaymentProviderSubmissionService's provider wiring is complete end to end -- but does
 * not yet call the real Huawei CPS SOAP API.
 *
 * <p>The pieces to build this on already exist (MoovMoneyXmlRequestBuilder, MoovMoneyWebClient,
 * Parser) but are deliberately not wired here yet because of one unresolved correctness
 * issue: MoovMoneyXmlRequestBuilder currently mints its own {@code OriginatorConversationID}
 * (a timestamp-based string) instead of accepting the caller's {@code paymentTransactionId} --
 * and {@code paymentTransactionId} is exactly what PaymentCallbackCorrelationService needs
 * echoed back in the later async callback to correlate it (see ProviderCallbackResult's
 * javadoc). Wiring this class to call the builder as-is would silently break correlation on
 * every real payment rather than fail loudly. That builder needs to accept an external
 * correlation reference first; that's the next real step here, not this skeleton.
 */
@Component
public class MoovPaymentProvider implements PaymentProvider {

    @Override
    public EPaymentOperator providerCode() {
        return EPaymentOperator.MOOV_MONEY;
    }

    @Override
    public ProviderPaymentResponse initiatePayment(ProviderPaymentRequest request) {
        return new ProviderPaymentResponse();
    }

    @Override
    public ProviderPaymentResponse checkPayment(String providerPaymentTransactionId) {
        return new ProviderPaymentResponse();
    }

}
