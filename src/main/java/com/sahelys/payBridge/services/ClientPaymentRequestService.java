package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.sahelys.payBridge.controllers.IPayBridgeController.ClientPaymentRequest;
import static com.sahelys.payBridge.controllers.IPayBridgeController.PaymentResponse;

/**
 * PM-03 Client API. Owns idempotent creation of the PaymentTransaction from a
 * ClientPaymentRequest and the shape of the PaymentResponse handed back to the Client --
 * not the transaction lifecycle itself, which stays in PaymentTransactionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientPaymentRequestService {

    private final PaymentTransactionService paymentTransactionService;

    @Value("${paybridge.public-base-url}")
    private String publicBaseUrl;

    public PaymentResponse submitClientPaymentRequest(ClientPaymentRequest request) {
        Optional<PaymentTransaction> existing = paymentTransactionService.findByClientIdentity(request.getClientAppId(), request.getClientPaymentRequestId());

        PaymentTransaction transaction = existing.isPresent()
                                         ? reuseExistingTransaction(existing.get(), request)
                                         : paymentTransactionService.create(request.getClientAppId(),
                                                                            request.getClientPaymentRequestId(),
                                                                            request.getAmount(),
                                                                            request.getCurrency(),
                                                                            request.getDescription(),
                                                                            request.getCallbackUrl());

        if (existing.isEmpty()) {
            log.info("Created PaymentTransaction {} for {}:{}", transaction.getId(), request.getClientAppId(), request.getClientPaymentRequestId());
        } else {
            log.info("Idempotent replay for {}:{} -> existing PaymentTransaction {}", request.getClientAppId(), request.getClientPaymentRequestId(), transaction.getId());
        }

        return PaymentResponse.builder()
                              .paymentTransactionId(transaction.getId())
                              .paymentUrl(publicBaseUrl + "/pay/" + transaction.getId())
                              .status(transaction.getStatus())
                              .build();
    }

    /**
     * Idempotency identity is clientAppId + clientPaymentRequestId (architecture-reference
     * section 24), enforced as a unique index at the database layer. A replay with the same
     * identity but a different amount/currency is rejected rather than silently accepted --
     * silently returning the original financial terms for materially different input would
     * hide a client-side bug from the caller.
     */
    private PaymentTransaction reuseExistingTransaction(PaymentTransaction existing, ClientPaymentRequest request) {
        boolean sameAmount = existing.getAmount().compareTo(request.getAmount()) == 0;
        boolean sameCurrency = existing.getCurrency().equals(request.getCurrency());
        if (!sameAmount || !sameCurrency)
            throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "clientPaymentRequestId " + request.getClientPaymentRequestId() + " was already submitted with different amount/currency");

        return existing;
    }
}
