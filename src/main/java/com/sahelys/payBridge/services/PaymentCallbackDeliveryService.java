package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.dto.ClientPaymentRequestCallback;
import com.sahelys.payBridge.domain.entities.ClientPaymentRequest;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.repository.ClientPaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackDeliveryService {

    private final RestClient restClient;
    private final ClientPaymentRequestRepository clientPaymentRequestRepository;

    /**
     * Builds the full callback content and attempts to POST it to the client's callbackUrl.
     * Always returns that content regardless of whether the POST itself succeeded -- what we
     * told/attempted to tell the client is valid, useful content either way; delivery
     * success/failure is only logged, never thrown (payment state is never affected by a
     * delivery failure).
     */
    public ClientPaymentRequestCallback deliverResponseToClientApp(PaymentTransaction transaction) {
        ClientPaymentRequest clientPaymentRequest = clientPaymentRequestRepository.findById(transaction.getClientPaymentRequestRef())
                                                                                   .orElseThrow(() -> new CustomException(EExceptionCode.ENTITY_NOT_FOUND,
                                                                                           "PaymentTransaction " + transaction.getId() + " references missing ClientPaymentRequest " + transaction.getClientPaymentRequestRef()));

        ClientPaymentRequestCallback callback = ClientPaymentRequestCallback.builder()
                                                                            .paymentTransactionId(transaction.getId())
                                                                            .clientAppId(clientPaymentRequest.getClientAppId())
                                                                            .clientPaymentRequestId(clientPaymentRequest.getClientPaymentRequestId())
                                                                            .amount(transaction.getAmount())
                                                                            .currency(transaction.getCurrency())
                                                                            .status(transaction.getStatus())
                                                                            .provider(transaction.getPaymentOperator())
                                                                            .providerPaymentTransactionId(transaction.getProviderPaymentTransactionId())
                                                                            .completedAt(Instant.now())
                                                                            .build();

        try {
            restClient.post()
                      .uri(transaction.getCallbackUrl())
                      .body(callback)
                      .retrieve()
                      .toBodilessEntity();
            log.info("Delivered ClientPaymentRequestCallback for {} to {}", transaction.getId(), transaction.getCallbackUrl());
        } catch (Exception ex) {
            log.error("Failed to deliverResponseToClientApp ClientPaymentRequestCallback for {} to {} -- payment state is unaffected", transaction.getId(), transaction.getCallbackUrl(), ex);
        }
        return callback;
    }

    /**
     * The explicit, payer-triggered action (e.g. a "Return to client app" button) -- unlike
     * "deliverResponseToClientApp" method, this is never called automatically by outcome application, since the
     * payer opening the paymentUrl isn't necessarily the same session that originated the
     * request on the client app.
     */
    public ClientPaymentRequestCallback notifyClientApp(PaymentTransaction transaction) {
        if (!transaction.getStatus().isTerminal())
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE,
                    "PaymentTransaction " + transaction.getId() + " is not yet terminal (" + transaction.getStatus() + ") -- nothing to notify the client about");

        return deliverResponseToClientApp(transaction);
    }
}
