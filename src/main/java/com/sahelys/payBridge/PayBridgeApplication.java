package com.sahelys.payBridge;

import com.sahelys.payBridge.domain.builder.HuaweiSoapRequestBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.LocalDateTime;

@SpringBootApplication @RequiredArgsConstructor @Slf4j
public class PayBridgeApplication {

    private final HuaweiSoapRequestBuilder huaweiSoapRequestBuilder;

    public static void main(String[] args) {
        SpringApplication.run(PayBridgeApplication.class, args);
    }

    @PostConstruct
    public void runAfterStartup() {
        log.info(huaweiSoapRequestBuilder.buildMerchantPaymentRequest("24166778899", String.valueOf(1500)));
        log.info(huaweiSoapRequestBuilder.buildGiveChangeRequest("24166778899", String.valueOf(500)));
        log.info(huaweiSoapRequestBuilder.buildQueryOrganizationBalanceRequest());

        LocalDateTime startDate = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endDate = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        log.info(huaweiSoapRequestBuilder.buildSearchTransactionByExtIdRequest(startDate, endDate));
    }
}
