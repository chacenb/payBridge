package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.domain.dto.ProviderCallbackResult;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;

/**
 * Turns one operator's raw async callback body into the provider-agnostic
 * {@link ProviderCallbackResult} that {@code PaymentCallbackCorrelationService} needs.
 * Which concrete parser runs is decided by which HTTP endpoint received the callback
 * (each operator has its own callback path) -- not by any dynamic lookup here.
 */
public interface CallbackParser {

//    ProviderCallbackResult parse(String rawPayload);
    ProviderCallbackResult parse(EPaymentOperator operator, String rawPayload);
}
