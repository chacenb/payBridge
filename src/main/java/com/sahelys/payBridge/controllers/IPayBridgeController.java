package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RequestMapping("/api/paybridge")
public interface IPayBridgeController {

    /**
     * PM-03 Client API -- architecture-reference/payment_communication_architecture_reference.md
     * section 28. This is the sole public entry point for a Client Application; it must expose
     * no provider-specific detail (guardrail 6).
     */
    @PostMapping("/v1/submit-payment-request")
    WsResponse<?> submitClientPaymentRequest(@Valid @RequestBody ClientPaymentRequest request);

    @PostMapping("/v1/payments/merchant")
    WsResponse<?> initiatePaymentProcess(@RequestBody PaymentRequest request);

    @PostMapping("/v1/transactions/search")
    WsResponse<?> searchTransactionByExternalId(@RequestBody SearchTransactionRequest request);

    @PostMapping("/v1/payments/give-change")
    WsResponse<?> giveChange(@RequestBody GiveChangeRequest request);


    /*--------------------------------------------------------*/
    /* Request models ----------------------------------------*/
    /*--------------------------------------------------------*/
    @Getter @Setter @ToString @Builder
    public class PaymentRequest {
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

    @Getter @Setter @ToString
    public class ClientPaymentRequest {

        @NotBlank
        private String clientAppId;

        @NotBlank
        private String clientPaymentRequestId;

        @NotNull
        @DecimalMin(value = "0.01", message = "amount must be strictly positive")
        private BigDecimal amount;

        @NotBlank
        @Size(min = 3, max = 3, message = "currency must be a 3-letter ISO 4217 code")
        private String currency;

        @Size(max = 512)
        private String description;

        @NotBlank
        @URL(message = "callbackUrl must be a valid URL")
        private String callbackUrl;
    }

    /*--------------------------------------------------------*/
    /* Response models ----------------------------------------*/
    /*--------------------------------------------------------*/

    @Getter @Setter @ToString @Builder
    public class PaymentResponse {
        private UUID                      paymentTransactionId;
        private String                        paymentUrl;
        private EPaymentTransactionStatusCode status;
    }

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
