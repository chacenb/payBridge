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
     * @return true if the Client endpoint acknowledged the callback (2xx), false otherwise.
     */
    public boolean deliver(PaymentTransaction transaction) {
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
