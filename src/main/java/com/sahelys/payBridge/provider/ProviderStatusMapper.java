package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.domain.enums.EProviderPaymentResultCode;

/**
 * architecture-reference/payment_communication_architecture_reference.md section 14:
 * <pre>
 *   MOOV SUCCESS -> PaymentTransaction SUCCESS
 *   MOOV FAILED  -> PaymentTransaction FAILED
 *   MOOV PENDING -> PaymentTransaction PROCESSING
 * </pre>
 * Frozen here once so no provider integration re-derives its own mapping (guardrail 6:
 * provider-specific status terminology never leaks past this class).
 */
public final class ProviderStatusMapper {

    private ProviderStatusMapper() {
    }

    public static EPaymentTransactionStatusCode toLocalTransactionStatus(EProviderPaymentResultCode providerPaymentResultCode) {
        return switch (providerPaymentResultCode) {
            case SUCCESS -> EPaymentTransactionStatusCode.SUCCESS;
            case FAILED -> EPaymentTransactionStatusCode.FAILED;
            case PENDING -> EPaymentTransactionStatusCode.PROCESSING;
        };
    }
}
