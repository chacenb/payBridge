package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.dto.ProviderPaymentRequest;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import org.springframework.stereotype.Component;

/**
 * Bare skeleton: registers AIRTEL as a real, callable operator so
 * PaymentProviderSubmissionService's provider wiring is complete end to end.
 *
 * <p>No real Airtel integration exists yet -- no captured request/response samples, no
 * client, no confirmed auth mechanism. Do not fill this in with a guessed API shape; it
 * needs the same kind of real captured evidence Moov had in _MATERIALS before a real
 * implementation can be written here.
 */
@Component
public class AirtelPaymentProvider implements PaymentProvider {

    @Override
    public EPaymentOperator providerCode() {
        return EPaymentOperator.AIRTEL_MONEY;
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
