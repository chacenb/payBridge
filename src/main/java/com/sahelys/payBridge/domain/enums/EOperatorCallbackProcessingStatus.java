package com.sahelys.payBridge.domain.enums;

/**
 * Matches the CHECK constraint on operator_callbacks.processing_status exactly
 * (V1__create_operator_callbacks.sql) -- values, not names, are what the DB constrains.
 */
public enum EOperatorCallbackProcessingStatus {
    RECEIVED,
    PROCESSED,
    REJECTED,
    FAILED
}
