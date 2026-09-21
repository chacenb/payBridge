package com.sahelys.payBridge.services;

import com.sahelys.payBridge.controllers.IPayBridgeController.MerchantPaymentResponse;
import com.sahelys.payBridge.domain.builder.MoovMoneyXmlRequestBuilder;
import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import com.sahelys.payBridge.domain.parser.Parser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class PayBridgeService {

    private final MoovMoneyXmlRequestBuilder moovMoneyXmlRequestBuilder;
    private final MoovMoneyWebClient         moovMoneyWebClient;
    private final Parser                     parser;

    public WsResponse<?> initiatePaymentProcess(String customerMsisdn, String amount) {
        String merchantPaymentBodyRequest = moovMoneyXmlRequestBuilder.buildMerchantPaymentRequest(customerMsisdn, amount);
        String xmlStringResult = moovMoneyWebClient.sendRequest(EHuaweiEndpointType.ASYNC, merchantPaymentBodyRequest);
        MerchantPaymentResponse response = parser.parseMerchantPaymentResponse(xmlStringResult);
        return WsResponse.<MerchantPaymentResponse>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .message("content inside data")
                         .data(response)
                         .build();
    }

    public WsResponse<?> giveChange(String customerMsisdn, String amount) {
        return null;
    }

    public WsResponse<?> queryOrganizationBalance() {
        return null;
    }

    public WsResponse<?> searchTransactionByExternalId(LocalDateTime startDate, LocalDateTime endDate) {
        return null;
    }
}
