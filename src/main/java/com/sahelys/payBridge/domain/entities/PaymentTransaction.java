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
 * The Payment Module's own payment identity -- architecture-reference guardrail 2. Created
 * from a {@link ClientPaymentRequest} (referenced via {@code clientPaymentRequestRef}) but
 * owns its own independent lifecycle and, later, the chosen provider -- it never owns the
 * Client's business transaction (guardrail 7). {@code clientAppId}/{@code clientPaymentRequestId}
 * live only on {@link ClientPaymentRequest}, not here.
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
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PaymentTransaction {

    @Id
    private UUID id;

    /**
     * Foreign key to {@link ClientPaymentRequest#getId()} -- the request's own surrogate
     * UUID primary key, NOT the client's external {@code clientPaymentRequestId} (a String,
     * lives only on {@link ClientPaymentRequest}). Named {@code Ref} rather than {@code Id}
     * specifically so it's never mistaken for that different, differently-typed field.
     */
    @Column(name = "client_payment_request_ref", nullable = false)
    private UUID clientPaymentRequestRef;

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
