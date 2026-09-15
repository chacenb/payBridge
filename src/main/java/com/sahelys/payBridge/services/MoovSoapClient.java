package com.sahelys.payBridge.services;

import com.sahelys.payBridge.domain.enums.HuaweiEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

@Component @Slf4j @RequiredArgsConstructor
public class MoovSoapClient {

    private final WebServiceTemplate webServiceTemplate;

    @Value("${momo.base-url}")
    private String baseUrl;

    @Value("${momo.payment-endpoint}")
    private String paymentEndpoint;

    @Value("${momo.sync-endpoint}")
    private String syncEndpoint;

    public String sendRequest(HuaweiEndpoint endpoint, String xml) {
        String url = switch(endpoint) {
            case PAYMENT -> baseUrl + paymentEndpoint;
            case SYNC -> baseUrl + syncEndpoint;
        };
        return send(url, xml);
    }

    private String send(String endpoint, String xml) {
        log.info("Calling Huawei SOAP endpoint : {}", endpoint);
        log.info("SOAP Request\n{}", xml);

        StringWriter writer = new StringWriter();

        webServiceTemplate.sendSourceAndReceiveToResult(
                endpoint,
                new StringSource(xml),
                new StreamResult(writer));

        String response = writer.toString();

        log.info("SOAP Response\n{}", response);
        return response;
    }


}
