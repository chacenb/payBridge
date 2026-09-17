package com.sahelys.payBridge.controllers.interfaces;

import com.sahelys.payBridge.domain.dto.WsResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDateTime;

@RequestMapping("/api/moov")
public interface IMoovMoneyGatewayController {

    @PostMapping("/v1/transactions/search")
    WsResponse<?> searchTransactionByExternalId(@RequestBody SearchTransactionRequest request);

//    @GetMapping("/v1/query-organization-balance")
//    WsResponse<?> queryOrganizationBalance();

    @PostMapping("/v1/payments/merchant")
    WsResponse<?> onlineMerchantPayment(@RequestBody MerchantPaymentRequest request);

    @PostMapping("/v1/payments/give-change")
    WsResponse<?> giveChange(@RequestBody GiveChangeRequest request);


    /*--------------------------------------------------------*/
    /* Request models ----------------------------------------*/
    /*--------------------------------------------------------*/
    @Getter @Setter @ToString @Builder
    public class MerchantPaymentRequest {
        private String customerMsisdn;
        private String amount;
    }

    @Getter @Setter @ToString @Builder
    public class GiveChangeRequest {
        private String customerMsisdn;
        private String amount;
    }

    @Getter @Setter @ToString @Builder
    public class SearchTransactionRequest {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    /*--------------------------------------------------------*/
    /* Response models ----------------------------------------*/
    /*--------------------------------------------------------*/
    @Getter @Setter @ToString @Builder
    public class MerchantPaymentResponse {
        private String  originatorConversationId;
        private String  conversationId;
        private Integer responseCode;
        private String  responseDescription;
        private Integer serviceStatus;
        private boolean accepted;

    }

}
