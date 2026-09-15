package com.sahelys.payBridge.services.interfaces;

import com.sahelys.payBridge.domain.dto.WsResponse;

import java.time.LocalDateTime;

public interface IMoovMoneyGatewayService {

    WsResponse<?> onlineMerchantPayment(String customerMsisdn, String amount);

    WsResponse<?> giveChange(String customerMsisdn, String amount);

    WsResponse<?> queryOrganizationBalance();

    WsResponse<?> searchTransactionByExternalId(LocalDateTime startDate, LocalDateTime endDate);

}
