package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Backend surface for the admin dashboard -- a separate audience from the public payment API
 * in {@link IPayBridgeController}, so it gets its own controller even though both currently
 * share the same "no auth yet" interim state.
 *
 * <p>No version on the class-level mapping, on purpose: each method carries its own {@code /v1/...}
 * (matching {@link IPayBridgeController}'s existing convention), so a future {@code /v2/...}
 * method can live alongside an existing {@code /v1/...} one in this same controller instead of
 * forcing a whole new class.
 */
@RequestMapping("/api/paybridge/admin")
public interface IAdminController {

    /**
     * Local-DB-only listing -- no provider call. Deliberately capped/paginated rather than
     * returning every row: an admin dashboard is exactly the kind of caller that will otherwise
     * eventually ask for "all of them" against a table that's grown well past what's reasonable
     * to return in one response.
     */
    @GetMapping("/v1/payment-transactions")
    WsResponse<?> listPaymentTransactions(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size);

    /*--------------------------------------------------------*/
    /* Response models ----------------------------------------*/
    /*--------------------------------------------------------*/

    /**
     * Lighter than {@link IPayBridgeController.PaymentPageResponse} -- drops
     * {@code availableProviders} (a static, per-request list, meaningless repeated across every
     * row of a listing).
     */
    @Getter @Setter @ToString @Builder
    public class PaymentTransactionSummary {
        private UUID                          paymentTransactionId;
        private BigDecimal                    amount;
        private String                        currency;
        private String                        description;
        private EPaymentTransactionStatusCode status;
        private EPaymentOperator              selectedProvider;
        private OffsetDateTime                createdAt;
    }

}
