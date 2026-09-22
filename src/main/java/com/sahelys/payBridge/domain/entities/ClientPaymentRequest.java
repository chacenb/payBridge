package com.sahelys.payBridge.domain.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * The Client Application's own request identity -- architecture-reference guardrail 1.
 * Durable, immutable record of exactly what the client submitted. {@link PaymentTransaction}
 * is created from one of these (via {@code clientPaymentRequestRef}) but owns its own,
 * independent lifecycle and provider selection -- it never mutates this record.
 */
@Entity
@Table(name = "client_payment_requests")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ClientPaymentRequest {

    @Id
    private UUID id;

    @Column(name = "client_app_id", nullable = false)
    private String clientAppId;

    @Column(name = "client_payment_request_id", nullable = false)
    private String clientPaymentRequestId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "description")
    private String description;

    @Column(name = "callback_url", nullable = false)
    private String callbackUrl;

    @Column(name = "received_at", insertable = false, updatable = false)
    private OffsetDateTime receivedAt;
}
