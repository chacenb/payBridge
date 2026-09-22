package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/paybridge")
public interface IPayBridgeController {

    @PostMapping("/v1/submit-payment-request")
    WsResponse<?> submitClientPaymentRequest(@Valid @RequestBody SubmitPaymentRequestBody request);

    /**
     * Backend data endpoint for the (separate, not-yet-built) Payment UI frontend -- it
     * fetches everything needed to render the payment page and let the payer pick a provider.
     * Deliberately not "/pay/{id}" (the browser-facing URL from the architecture doc's own
     * example): that route belongs to the frontend app, which calls this endpoint.
     */
    @GetMapping("/v1/payment-transactions/{paymentTransactionId}")
    WsResponse<?> getPaymentTransaction(@PathVariable UUID paymentTransactionId);

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
    public class SubmitPaymentRequestBody {

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

    /**
     * What the Payment UI frontend needs to render the payment page. Deliberately excludes
     * callbackUrl (the client's internal webhook, no browser-side use), clientAppId/
     * clientPaymentRequestId (the client's own identity -- lives on ClientPaymentRequest,
     * not needed here), and providerPaymentTransactionId (Huawei CPS-internal correlation
     * id -- provider-specific detail that shouldn't leak past the abstraction).
     */
    @Getter @Setter @ToString @Builder
    public class PaymentPageResponse {
        private UUID                          paymentTransactionId;
        private BigDecimal                    amount;
        private String                        currency;
        private String                        description;
        private EPaymentTransactionStatusCode status;
        private EPaymentOperator              selectedProvider;
        private List<EPaymentOperator>        availableProviders;
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
