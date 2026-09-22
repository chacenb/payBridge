package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.services.ClientPaymentRequestService;
import com.sahelys.payBridge.services.PayBridgeService;
import com.sahelys.payBridge.services.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PayBridgeController implements IPayBridgeController {

    private final PayBridgeService            payBridgeService;
    private final ClientPaymentRequestService clientPaymentRequestService;
    private final PaymentTransactionService   paymentTransactionService;

    @Override
    public WsResponse<?> submitClientPaymentRequest(SubmitPaymentRequestBody request) {
        PaymentResponse response = clientPaymentRequestService.submitClientPaymentRequest(request);
        return WsResponse.<PaymentResponse>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .data(response)
                         .build();
    }

    @Override
    public WsResponse<?> getPaymentTransaction(UUID paymentTransactionId) {
        PaymentTransaction transaction = paymentTransactionService.findById(paymentTransactionId);
        PaymentPageResponse response = PaymentPageResponse.builder()
                                                          .paymentTransactionId(transaction.getId())
                                                          .amount(transaction.getAmount())
                                                          .currency(transaction.getCurrency())
                                                          .description(transaction.getDescription())
                                                          .status(transaction.getStatus())
                                                          .selectedProvider(transaction.getProvider())
                                                          .availableProviders(List.of(EPaymentOperator.values()))
                                                          .build();
        return WsResponse.<PaymentPageResponse>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .data(response)
                         .build();
    }

    @Override
    public WsResponse<?> searchTransactionByExternalId(SearchTransactionRequest request) {
        return payBridgeService.searchTransactionByExternalId(request.getStartDate(), request.getEndDate());
    }

    @Override
    public WsResponse<?> initiatePaymentProcess(PaymentRequest request) {
        return payBridgeService.initiatePaymentProcess(request.getCustomerMsisdn(), request.getAmount());
    }

    @Override
    public WsResponse<?> giveChange(GiveChangeRequest request) {
        return payBridgeService.giveChange(request.getCustomerMsisdn(), request.getAmount());
    }

}
