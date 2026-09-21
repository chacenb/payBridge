package com.sahelys.payBridge.domain.enums;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Mirrors architecture-reference/payment_communication_architecture_reference.md section 23.
 * SUCCESS, FAILED, CANCELLED and EXPIRED are terminal: no further transition is legal from them.
 */
public enum EPaymentTransactionStatusCode {

    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    CANCELLED,
    EXPIRED;

    private static final Map<EPaymentTransactionStatusCode, Set<EPaymentTransactionStatusCode>> LEGAL_TRANSITIONS = Map.of(
            PENDING,    EnumSet.of(PROCESSING, CANCELLED, EXPIRED, FAILED),
            PROCESSING, EnumSet.of(SUCCESS, FAILED, CANCELLED, EXPIRED),
            SUCCESS,    EnumSet.noneOf(EPaymentTransactionStatusCode.class),
            FAILED,     EnumSet.noneOf(EPaymentTransactionStatusCode.class),
            CANCELLED,  EnumSet.noneOf(EPaymentTransactionStatusCode.class),
            EXPIRED,    EnumSet.noneOf(EPaymentTransactionStatusCode.class)
                                                                                                                          );

    public boolean canTransitionTo(EPaymentTransactionStatusCode target) {
        return LEGAL_TRANSITIONS.get(this).contains(target);
    }

    public boolean isTerminal() {
        return LEGAL_TRANSITIONS.get(this).isEmpty();
    }
}
