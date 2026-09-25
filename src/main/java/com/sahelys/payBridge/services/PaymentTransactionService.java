package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.ClientPaymentRequest;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * PM-02 Transaction Core: creation, retrieval and controlled state transitions only.
 * Idempotent create-or-get semantics belong to the Client API / idempotency steps built on
 * top of this service, not to the core lifecycle itself -- {@link #findByClientPaymentRequestRef}
 * is the lookup they will use.
 */
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentTransactionRepository repository;

    @Transactional
    public PaymentTransaction create(ClientPaymentRequest clientPaymentRequest) {
        PaymentTransaction transaction = PaymentTransaction.builder()
                                                           .id(UUID.randomUUID())
                                                           .clientPaymentRequestRef(clientPaymentRequest.getId())
                                                           .amount(clientPaymentRequest.getAmount())
                                                           .currency(clientPaymentRequest.getCurrency())
                                                           .description(clientPaymentRequest.getDescription())
                                                           .callbackUrl(clientPaymentRequest.getCallbackUrl())
                                                           .status(EPaymentTransactionStatusCode.PENDING)
                                                           .build();
        return repository.save(transaction);
    }

    public PaymentTransaction findById(UUID paymentTransactionId) {
        return repository.findById(paymentTransactionId).orElseThrow(() -> new CustomException(EExceptionCode.ENTITY_NOT_FOUND, "No PaymentTransaction with id " + paymentTransactionId));
    }

    /**
     * Same lookup as {@link #findById}, but for callers -- e.g. callback correlation -- that
     * must treat "no such transaction" as a normal, handled outcome rather than an error.
     */
    public Optional<PaymentTransaction> tryFindById(UUID paymentTransactionId) {
        return repository.findById(paymentTransactionId);
    }

    public Optional<PaymentTransaction> findByClientPaymentRequestRef(UUID clientPaymentRequestRef) {
        return repository.findByClientPaymentRequestRef(clientPaymentRequestRef);
    }

    /**
     * Well within Postgres's valid timestamptz range but far outside any real transaction's
     * date, so they're safe stand-ins for "no lower/upper bound" -- see {@link #findAll} below.
     */
    private static final OffsetDateTime NO_LOWER_BOUND = OffsetDateTime.parse("1970-01-01T00:00:00Z");
    private static final OffsetDateTime NO_UPPER_BOUND = OffsetDateTime.parse("2999-12-31T23:59:59Z");

    /**
     * Local-DB-only listing for the admin dashboard -- no provider call, no reconciliation.
     * {@code createdFrom}/{@code createdTo} are each optional independently; a null bound is
     * substituted with a sentinel far outside any real data rather than left as a null bind
     * parameter -- {@link PaymentTransactionRepository#findByCreatedAtBetween} explains why.
     */
    public Page<PaymentTransaction> findAll(OffsetDateTime createdFrom, OffsetDateTime createdTo, Pageable pageable) {
        OffsetDateTime from = createdFrom != null ? createdFrom : NO_LOWER_BOUND;
        OffsetDateTime to = createdTo != null ? createdTo : NO_UPPER_BOUND;
        return repository.findByCreatedAtBetween(from, to, pageable);
    }

    @Transactional
    public PaymentTransaction changeTransactionStatusTo(UUID paymentTransactionId, EPaymentTransactionStatusCode targetTransacStatuc) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.transitionTo(targetTransacStatuc);
        return repository.save(transaction);
    }

    @Transactional
    public PaymentTransaction attachPaymentOperatorToTransaction(UUID paymentTransactionId, EPaymentOperator provider) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.setPaymentOperator(provider);
        return repository.save(transaction);
    }

    @Transactional
    public PaymentTransaction attachProviderTransactionIdToLocalTransaction(UUID paymentTransactionId, String providerPaymentTransactionId) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.setProviderPaymentTransactionId(providerPaymentTransactionId);
        return repository.save(transaction);
    }
}
