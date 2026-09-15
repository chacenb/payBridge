package com.sahelys.payBridge.services;

import com.sahelys.payBridge.controllers.interfaces.IMoovMoneyGatewayController.MerchantPaymentResponse;
import com.sahelys.payBridge.domain.builder.HuaweiSoapRequestBuilder;
import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.HuaweiEndpoint;
import com.sahelys.payBridge.domain.parser.Parser;
import com.sahelys.payBridge.services.interfaces.IMoovMoneyGatewayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class MoovMoneyGatewayService implements IMoovMoneyGatewayService {

    private final HuaweiSoapRequestBuilder requestBuilder;
    private final MoovSoapClient           soapClient;
    private final Parser                   parser;

    @Override public WsResponse<?> onlineMerchantPayment(String customerMsisdn, String amount) {
        String paymentRequest = requestBuilder.buildMerchantPaymentRequest(customerMsisdn, amount);
        String xmlResult = soapClient.sendRequest(HuaweiEndpoint.PAYMENT, paymentRequest);
        MerchantPaymentResponse response = parser.parseMerchantPaymentResponse(xmlResult);
        return WsResponse.<MerchantPaymentResponse>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .message("content inside data")
                         .data(response)
                         .build();
    }

    @Override public WsResponse<?> giveChange(String customerMsisdn, String amount) {
        return null;
    }

    @Override public WsResponse<?> queryOrganizationBalance() {
        return null;
    }

    @Override public WsResponse<?> searchTransactionByExternalId(LocalDateTime startDate, LocalDateTime endDate) {
        return null;
    }
}
