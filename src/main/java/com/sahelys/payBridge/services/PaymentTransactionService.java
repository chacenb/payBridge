package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.ClientPaymentRequest;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public PaymentTransaction transitionPaymentTransactionStatus(UUID paymentTransactionId, EPaymentTransactionStatusCode targetTransacStatuc) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.transitionTo(targetTransacStatuc);
        return repository.save(transaction);
    }

    @Transactional
    public PaymentTransaction attachProvider(UUID paymentTransactionId, EPaymentOperator provider) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.setProvider(provider);
        return repository.save(transaction);
    }

    @Transactional
    public PaymentTransaction attachProviderTransactionId(UUID paymentTransactionId, String providerPaymentTransactionId) {
        PaymentTransaction transaction = findById(paymentTransactionId);
        transaction.setProviderPaymentTransactionId(providerPaymentTransactionId);
        return repository.save(transaction);
    }
}
