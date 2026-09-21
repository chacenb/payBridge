package com.sahelys.payBridge.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * architecture-reference/payment_communication_architecture_reference.md section 22.
 * What the Payment Module hands to a PaymentProvider; each implementation translates this
 * further into its own API/SOAP/REST model. No provider-specific field belongs here.
 */
@Getter
@Setter
@ToString
@Builder
public class ProviderPaymentRequest {
    private UUID paymentTransactionId;
    private BigDecimal amount;
    private String currency;
    private String customerPhone;
}
