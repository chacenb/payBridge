package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.controllers.interfaces.IMoovMoneyGatewayController;
import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.services.interfaces.IMoovMoneyGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MoovMoneyGatewayController implements IMoovMoneyGatewayController {

    private final IMoovMoneyGatewayService moovMoneyGatewayService;

    @Override
    public WsResponse<?> searchTransactionByExternalId(SearchTransactionRequest request) {
        return moovMoneyGatewayService.searchTransactionByExternalId(request.getStartDate(), request.getEndDate());
    }

//    @Override
    public WsResponse<?> queryOrganizationBalance() {
        return moovMoneyGatewayService.queryOrganizationBalance();
    }

    @Override
    public WsResponse<?> onlineMerchantPayment(MerchantPaymentRequest request) {
        return moovMoneyGatewayService.onlineMerchantPayment(request.getCustomerMsisdn(), request.getAmount());
    }

    @Override
    public WsResponse<?> giveChange(GiveChangeRequest request) {
        return moovMoneyGatewayService.giveChange(request.getCustomerMsisdn(), request.getAmount());
    }

}
