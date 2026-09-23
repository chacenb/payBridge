package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    /**
     * The explicit, payer-triggered "notify/return to client app" action -- e.g. a button on
     * the Payment UI once the payer is done, not called automatically by outcome application
     * (the payer opening paymentUrl isn't necessarily the client app's own session). Only
     * valid once the transaction is terminal.
     */
    @PostMapping("/v1/payment-transactions/{paymentTransactionId}/notify-client")
    WsResponse<?> notifyClient(@PathVariable UUID paymentTransactionId);

    /**
     * The payer's provider choice + phone number, submitted after viewing the payment page.
     * Delegates to PaymentProviderSubmissionService.submitToProvider -- see its own javadoc
     * for what happens synchronously (accept/reject) vs. later via the async callback.
     */
    @PostMapping("/v1/payment-transactions/{paymentTransactionId}/proceed-payment")
    WsResponse<?> submitPaymentTowardsProvider(@PathVariable UUID paymentTransactionId, @Valid @RequestBody SelectProviderRequest request);


    /*--------------------------------------------------------*/
    /* Request models ----------------------------------------*/
    /*--------------------------------------------------------*/
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

    @Getter @Setter @ToString
    public class SelectProviderRequest {

        @NotBlank
        private String providerCode;

        /**
         * Gabon MSISDN, grounded in the real production samples in
         * _MATERIALS/Endpoints_Moov_Money_PROD/ (InitTrans_OnlineMerchantPayment.xml,
         * InitTrans_GiveChange.xml -- both use "24166855158"): country code 241 + an 8-digit
         * subscriber number, 11 digits total, no "+", no leading "0". Backend is the source
         * of truth here -- never trust the frontend's own validation.
         */
        @NotBlank
        @Pattern(regexp = "^241[0-9]{8}$", message = "customerPhone must be a Gabon MSISDN in the format 241XXXXXXXX (11 digits)")
        private String customerPhone;
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

}
