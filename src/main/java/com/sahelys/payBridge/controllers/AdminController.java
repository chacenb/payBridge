package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.services.PaymentReconciliationService;
import com.sahelys.payBridge.services.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminController implements IAdminController {

    /**
     * Hard ceiling on {@code size}, independent of whatever the caller asks for -- a dashboard
     * requesting "all of them" must still get a bounded response, not an unbounded table dump.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentTransactionService    paymentTransactionService;
    private final PaymentReconciliationService reconciliationService;

    @Override
    public WsResponse<?> listPaymentTransactions(int page, int size, LocalDate startDate, LocalDate endDate) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        OffsetDateTime createdFrom = startDate == null ? null : startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime createdTo = endDate == null ? null : endDate.atTime(LocalTime.of(23, 59, 59)).atOffset(ZoneOffset.UTC);

        Page<PaymentTransaction> result = paymentTransactionService.findAll(createdFrom, createdTo, PageRequest.of(page, clampedSize));

        List<PaymentTransactionSummary> summaries = result.getContent().stream()
                                                          .map(this::toSummary)
                                                          .toList();

        return WsResponse.<List<PaymentTransactionSummary>>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .data(summaries)
                         .page(result.getNumber())
                         .total((int) result.getTotalElements())
                         .build();
    }

    @Override
    public WsResponse<?> reconcileWithProvider(UUID paymentTransactionId, LocalDate startDate, LocalDate endDate) {
        PaymentReconciliationService.ReconciliationOutcome outcome =
                reconciliationService.reconcileWithProvider(paymentTransactionId, startDate, endDate);

        ReconciliationResponse response = ReconciliationResponse.builder()
                                                                 .paymentTransactionId(outcome.transaction().getId())
                                                                 .localStatus(outcome.transaction().getStatus())
                                                                 .queriedProvider(outcome.queriedProvider())
                                                                 .providerResultCode(outcome.providerResultCode())
                                                                 .providerResultDesc(outcome.providerResultDesc())
                                                                 .providerCompletedAt(outcome.providerCompletedAt())
                                                                 .build();

        return WsResponse.<ReconciliationResponse>builder()
                         .timeStamp(ZonedDateTime.now())
                         .status(HttpStatus.OK)
                         .data(response)
                         .build();
    }

    private PaymentTransactionSummary toSummary(PaymentTransaction transaction) {
        return PaymentTransactionSummary.builder()
                                        .paymentTransactionId(transaction.getId())
                                        .amount(transaction.getAmount())
                                        .currency(transaction.getCurrency())
                                        .description(transaction.getDescription())
                                        .status(transaction.getStatus())
                                        .selectedProvider(transaction.getPaymentOperator())
                                        .createdAt(transaction.getCreatedAt())
                                        .build();
    }

}
