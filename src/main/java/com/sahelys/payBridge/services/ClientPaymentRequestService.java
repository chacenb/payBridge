package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.entities.ClientPaymentRequest;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.globals.exceptions.CustomException;
import com.sahelys.payBridge.globals.exceptions.EExceptionCode;
import com.sahelys.payBridge.repository.ClientPaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static com.sahelys.payBridge.controllers.IPayBridgeController.PaymentResponse;
import static com.sahelys.payBridge.controllers.IPayBridgeController.SubmitPaymentRequestBody;

/**
 * PM-03 Client API. Owns idempotent creation of the ClientPaymentRequest and its linked
 * PaymentTransaction, and the shape of the PaymentResponse handed back to the Client --
 * not the transaction lifecycle itself, which stays in PaymentTransactionService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientPaymentRequestService {

    private final ClientPaymentRequestRepository clientPaymentRequestRepository;
    private final PaymentTransactionService      paymentTransactionService;

    @Value("${paybridge.public-base-url}")
    private String publicBaseUrl;

    @Transactional
    public PaymentResponse submitClientPaymentRequest(SubmitPaymentRequestBody request) {
        Optional<ClientPaymentRequest> existing = clientPaymentRequestRepository.findByClientAppIdAndClientPaymentRequestId(request.getClientAppId(), request.getClientPaymentRequestId());

        PaymentTransaction transaction = existing.isPresent()
                                         ? reuseExistingTransaction(existing.get(), request)
                                         : createRequestAndTransaction(request);

        if (existing.isEmpty()) log.info("Created PaymentTransaction {} for {}:{}", transaction.getId(), request.getClientAppId(), request.getClientPaymentRequestId());
        else log.info("___Idempotent replay for {}:{} -> Already existing PaymentTransaction {}", request.getClientAppId(), request.getClientPaymentRequestId(), transaction.getId());

        return PaymentResponse.builder()
                              .paymentTransactionId(transaction.getId())
                              // The frontend SPA uses HashLocationStrategy (app.config.ts) --
                              // routes are only reachable via the #/... fragment, not a real path.
                              .paymentUrl(publicBaseUrl + "/#/pay/" + transaction.getId())
                              .status(transaction.getStatus())
                              .build();
    }

    private PaymentTransaction createRequestAndTransaction(SubmitPaymentRequestBody request) {
        ClientPaymentRequest clientPaymentRequest = clientPaymentRequestRepository.save(
                ClientPaymentRequest.builder()
                                    .id(UUID.randomUUID())
                                    .clientAppId(request.getClientAppId())
                                    .clientPaymentRequestId(request.getClientPaymentRequestId())
                                    .amount(request.getAmount())
                                    .currency(request.getCurrency())
                                    .description(request.getDescription())
                                    .callbackUrl(request.getCallbackUrl())
                                    .build());
        return paymentTransactionService.create(clientPaymentRequest);
    }

    /**
     * Idempotency identity is clientAppId + clientPaymentRequestId (architecture-reference
     * section 24), enforced as a unique index at the database layer, on ClientPaymentRequest.
     * A replay with the same identity but a different amount/currency is rejected rather than
     * silently accepted -- silently returning the original financial terms for materially
     * different input would hide a client-side bug from the caller.
     */
    private PaymentTransaction reuseExistingTransaction(ClientPaymentRequest oldPaymentRequest, SubmitPaymentRequestBody newPaymentRequest) {
        boolean sameAmount = oldPaymentRequest.getAmount().compareTo(newPaymentRequest.getAmount()) == 0;
        boolean sameCurrency = oldPaymentRequest.getCurrency().equals(newPaymentRequest.getCurrency());
        if (!sameAmount || !sameCurrency) throw new CustomException(EExceptionCode.DATA_INCOHERENCE, "clientPaymentRequestId " + newPaymentRequest.getClientPaymentRequestId() + " was already submitted with different amount/currency");

        return paymentTransactionService.findByClientPaymentRequestRef(oldPaymentRequest.getId())
                                        .orElseThrow(() -> new CustomException(EExceptionCode.ENTITY_NOT_FOUND, "ClientPaymentRequest " + oldPaymentRequest.getId() + " has no linked PaymentTransaction"));
    }
}
