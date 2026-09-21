package com.sahelys.payBridge.domain.entities;

import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * The Payment Module's own payment identity -- architecture-reference guardrail 2.
 * Owns clientAppId/clientPaymentRequestId (the Client's identity) without owning the
 * Client's business transaction (guardrail 7).
 *
 * <p>MVP simplification: absorbs {@code provider}/{@code providerPaymentTransactionId}
 * directly (no separate PaymentAttempt entity -- collapsed for the MVP since nothing live
 * needs to represent more than one provider try per transaction yet). Both are null until a
 * provider is selected/acknowledges; only one provider's info can live here at a time, so a
 * same-transaction retry against a second provider would overwrite the first provider's
 * reference -- a real limitation to revisit if that retry story becomes necessary.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EPaymentTransactionStatusCode status;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private EPaymentOperator provider;

    @Column(name = "provider_payment_transaction_id")
    private String providerPaymentTransactionId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * Enforces the state machine from the architecture reference so that no caller --
     * service, callback correlation, or reconciliation job -- can drive the transaction
     * through an illegal transition.
     */
    public void transitionTo(EPaymentTransactionStatusCode target) {
        if (!status.canTransitionTo(target)) {
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                    "Illegal PaymentTransaction transition: " + status + " -> " + target);
        }
        this.status = target;
    }
}
