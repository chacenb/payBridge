package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.services.ClientPaymentRequestService;
import com.sahelys.payBridge.services.PayBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;

@RestController
@RequiredArgsConstructor
public class PayBridgeController implements IPayBridgeController {

    private final PayBridgeService            payBridgeService;
    private final ClientPaymentRequestService clientPaymentRequestService;

    @Override
    public WsResponse<?> submitClientPaymentRequest(ClientPaymentRequest request) {
        PaymentResponse response = clientPaymentRequestService.submitClientPaymentRequest(request);
        return WsResponse.<PaymentResponse>builder()
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
