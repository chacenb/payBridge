package com.sahelys.payBridge.domain.enums;

/**
 * Observable result of PM-07.1 correlation, so duplicate/unknown callbacks are distinguishable
 * from a real state change rather than all looking like silent success.
 */
public enum ECorrelationOutcome {
    APPLIED,
    DUPLICATE_IGNORED,
    UNKNOWN_PROVIDER_TRANSACTION
}
