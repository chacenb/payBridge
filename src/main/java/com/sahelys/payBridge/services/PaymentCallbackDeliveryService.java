package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.dto.ClientPaymentRequestCallback;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
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

    /**
     * @return true if the Client endpoint acknowledged the callback (2xx), false otherwise.
     */
    public boolean deliver(PaymentTransaction transaction) {
        ClientPaymentRequestCallback callback = ClientPaymentRequestCallback.builder()
                                                                            .paymentTransactionId(transaction.getId())
                                                                            .clientAppId(transaction.getClientAppId())
                                                                            .clientPaymentRequestId(transaction.getClientPaymentRequestId())
                                                                            .amount(transaction.getAmount())
                                                                            .currency(transaction.getCurrency())
                                                                            .status(transaction.getStatus())
                                                                            .provider(transaction.getProvider())
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
            return true;
        } catch (Exception ex) {
            log.error("Failed to deliver ClientPaymentRequestCallback for {} to {} -- payment state is unaffected", transaction.getId(), transaction.getCallbackUrl(), ex);
            return false;
        }
    }
}
