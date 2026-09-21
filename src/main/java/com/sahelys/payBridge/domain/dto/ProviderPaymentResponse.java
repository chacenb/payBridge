package com.sahelys.payBridge.domain.dto;

import com.sahelys.payBridge.domain.enums.EProviderPaymentResultCode;
import lombok.*;

/**
 * architecture-reference/payment_communication_architecture_reference.md section 22.
 * What a PaymentProvider hands back immediately after initiatePaymentProcess/checkPayment --
 * an acknowledgement or a query result, never the guaranteed financial outcome.
 */
@Getter @Setter @ToString @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderPaymentResponse {
    private String                     providerPaymentTransactionId;
    private EProviderPaymentResultCode status;
    private String                     message;
}
