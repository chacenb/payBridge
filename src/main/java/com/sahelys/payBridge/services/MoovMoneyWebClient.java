package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.enums.EHuaweiEndpointType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component @Slf4j @RequiredArgsConstructor
public class MoovMoneyWebClient {

    @Value("${momo.base-url}")
    private String baseUrl;

    @Value("${momo.payment-endpoint}")
    private String asyncEndpoint;

    @Value("${momo.sync-endpoint}")
    private String syncEndpoint;

    public String sendRequest(EHuaweiEndpointType endpoint, String xmlString) {
        String url = switch (endpoint) {
            case ASYNC -> baseUrl + asyncEndpoint;
            case SYNC -> baseUrl + syncEndpoint;
        };

        log.info("Calling Huawei SOAP url : {}", url);
        log.info("SOAP Request\n{}", xmlString);

        /* Make the actual call */

        String response = null;

        log.info("SOAP Response\n{}", response);
        return response;
    }


//    public String sendRequest(EHuaweiEndpointType endpoint, String xml) {
//        String url = switch(endpoint) {
//            case PAYMENT -> baseUrl + paymentEndpoint;
//            case SYNC -> baseUrl + syncEndpoint;
//        };
//        return send(url, xml);
//    }
//
//    private String send(String endpoint, String xml) {
//        log.info("Calling Huawei SOAP endpoint : {}", endpoint);
//        log.info("SOAP Request\n{}", xml);
//
//        StringWriter writer = new StringWriter();
//
//        webServiceTemplate.sendSourceAndReceiveToResult(
//                endpoint,
//                new StringSource(xml),
//                new StreamResult(writer));
//
//        String response = writer.toString();
//
//        log.info("SOAP Response\n{}", response);
//        return response;
//    }


}
