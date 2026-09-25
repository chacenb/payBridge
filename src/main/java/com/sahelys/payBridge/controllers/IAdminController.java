package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.enums.EPaymentOperator;
import com.sahelys.payBridge.domain.enums.EPaymentTransactionStatusCode;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
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
     * to return in one response. {@code startDate}/{@code endDate} are optional -- omitted,
     * the listing is unfiltered by date; supplied, rows are filtered by {@code createdAt} falling
     * within that (inclusive) window.
     */
    @GetMapping("/v1/payment-transactions")
    WsResponse<?> listPaymentTransactions(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

    /**
     * Forces a live {@code SearchTransactionByExtID} query against Moov and surfaces exactly
     * what it says, without ever silently trusting it or interpreting it: the real captured
     * response for this command has no success/failure field, only a result code/description
     * for the search itself and when the transaction completed (see
     * {@link com.sahelys.payBridge.provider.ProviderXmlParser#parseMoovSearchTransactionResult}),
     * so the local transaction's status is never changed by this call, and the response carries
     * those raw fields as-is rather than a derived flag. {@code startDate}/{@code endDate} are
     * the search window sent to Moov -- the admin's call, not derived/guessed here.
     */
    @GetMapping("/v1/payment-transactions/{paymentTransactionId}/search-with-provider")
    WsResponse<?> reconcileWithProvider(@PathVariable UUID paymentTransactionId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate);

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

    /**
     * {@code localStatus} is always the transaction's actual, unchanged local status --
     * {@code reconcileWithProvider} never mutates it. {@code queriedProvider} is false only when
     * the transaction was already terminal locally (provider not called at all) -- in that case
     * the three {@code provider*} fields are null. Otherwise they're exactly the raw
     * {@code ResultCode}/{@code ResultDesc}/{@code BOCompletedTime} Moov returned, not
     * interpreted into a success/failure verdict -- see
     * {@link com.sahelys.payBridge.provider.ProviderXmlParser#parseMoovSearchTransactionResult}
     * for why.
     */
    @Getter @Setter @ToString @Builder
    public class ReconciliationResponse {
        private UUID                          paymentTransactionId;
        private EPaymentTransactionStatusCode localStatus;
        private boolean                       queriedProvider;
        private String                        providerResultCode;
        private String                        providerResultDesc;
        private String                        providerCompletedAt;
    }

}
