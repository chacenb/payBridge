package com.sahelys.payBridge.domain.enums;

import com.sahelys.payBridge.provider.PaymentProvider;

/**
 * The known, fixed universe of mobile money operators PayBridge is designed to support --
 * MOOV (confirmed against the real captures in _MATERIALS) and AIRTEL. This is the single
 * place that list is declared -- adding an operator is a one-line change here, matched by
 * the {@code providerCode()} of its {@link PaymentProvider} implementation.
 */
public enum EPaymentOperator {
    MOOV_MONEY,
    AIRTEL_MONEY,


}
