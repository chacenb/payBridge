package com.sahelys.payBridge.provider;

import com.sahelys.payBridge.controllers.IPayBridgeController;
import com.sahelys.payBridge.domain.dto.ProviderPaymentRequest;
import com.sahelys.payBridge.domain.dto.ProviderPaymentResponse;
import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * PM-05 generic provider boundary -- architecture-reference section 11 / guardrail 8.
 * Transaction Core, the Client API and the callback engine depend on this interface only;
 * no caller may reference a concrete provider (e.g. MoovPaymentProvider) directly.
 *
 * <p>Confirmed against the real Moov captures in _MATERIALS, a transaction-initiating call
 * (e.g. InitTrans_OnlineMerchantPayment, InitTrans_GiveChange) is a 3-step shape:
 * <pre>
 *   1. call            -- we send the request, minting our own correlation reference
 *   2. sync ack        -- immediate HTTP response: "accepted" or "rejected", never the
 *                          real financial outcome
 *   3. async response   -- later, independent POST to our callback URL carrying the real
 *                          result, echoing our correlation reference back
 * </pre>
 * {@link #initiatePayment} is steps 1-2 only. For an async provider like Moov it must
 * resolve to {@code EProviderPaymentResultCode.PENDING} (accepted, real outcome pending) or
 * {@code FAILED} (rejected at submission) -- never {@code SUCCESS}; step 3 always arrives
 * later, through {@link com.sahelys.payBridge.services.PaymentCallbackCorrelationService}.
 * A provider that is genuinely fully synchronous may legitimately return {@code SUCCESS}
 * here instead -- {@link com.sahelys.payBridge.services.PaymentOutcomeApplier}
 * handles either case identically.
 *
 * <p>Not in scope: plain synchronous query commands (e.g. SearchTransactionByExtID, and
 * likely QueryOrganizationBalance) have no async leg at all and do not belong behind this
 * interface -- they are reconciliation/query concerns, not payment initiation.
 */
public interface PaymentProvider {

    /**
     * The operator this provider implements, from the fixed set declared in
     * {@link EPaymentOperator}. Used by
     * {@link com.sahelys.payBridge.services.PaymentProviderSubmissionService} to resolve a
     * PaymentTransaction's provider string to this instance.
     */
    EPaymentOperator providerCode();

    ProviderPaymentResponse initiatePayment(ProviderPaymentRequest request);

    /**
     * Queries the provider for the current status of a previously initiated payment,
     * identified by the provider's own transaction id. Used for reconciliation when no
     * callback has arrived within the expected window.
     */
    ProviderPaymentResponse checkPayment(String providerPaymentTransactionId);

    ProviderPaymentResponse giveChange();

}
