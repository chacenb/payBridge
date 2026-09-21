package com.sahelys.payBridge.domain.dto;

import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * architecture-reference/payment_communication_architecture_reference.md section 16 --
 * guardrail 5. The normalized contract delivered to the Client Application's callbackUrl.
 * Provider-specific status terminology must never reach this DTO (guardrail 6).
 */
@Getter
@Setter
@ToString
@Builder
public class ClientPaymentRequestCallback {
    private String                    clientAppId;
    private String                    clientPaymentRequestId;
    private UUID                      paymentTransactionId;
    private BigDecimal                amount;
    private String                        currency;
    private EPaymentTransactionStatusCode status;
    private EPaymentOperator              provider;
    private String                    providerPaymentTransactionId;
    private Instant                   completedAt;
}
