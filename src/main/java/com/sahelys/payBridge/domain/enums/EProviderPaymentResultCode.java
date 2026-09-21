package com.sahelys.payBridge.domain.enums;

/**
 * Internal to the provider integration layer -- architecture-reference section 23.
 * Never exposed to the Client Application; the Payment Module maps this to
 * EPaymentTransactionStatusCode before anything crosses that boundary.
 */
public enum EProviderPaymentResultCode {
    SUCCESS,
    FAILED,
    PENDING,
}
