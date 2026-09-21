package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Explicit MOOV/AIRTEL -> PaymentProvider matching, spelled out here rather than assembled
 * dynamically from whatever PaymentProvider beans happen to exist. Adding a third operator
 * means adding a branch here, by hand, next to the other two -- not registering a bean and
 * hoping it gets picked up.
 */
@Component
@RequiredArgsConstructor
public class ProviderMatcher {

    private final MoovPaymentProvider moovPaymentProvider;
    private final AirtelPaymentProvider airtelPaymentProvider;

    public PaymentProvider match(EPaymentOperator operator) {
        return switch (operator) {
            case MOOV_MONEY -> moovPaymentProvider;
            case AIRTEL_MONEY -> airtelPaymentProvider;
        };
    }
}
