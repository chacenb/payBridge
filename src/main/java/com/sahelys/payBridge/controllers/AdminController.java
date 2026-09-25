package com.sahelys.payBridge.controllers;

import com.sahelys.payBridge.domain.dto.WsResponse;
import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import com.sahelys.payBridge.services.PaymentTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZonedDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminController implements IAdminController {

    /**
     * Hard ceiling on {@code size}, independent of whatever the caller asks for -- a dashboard
     * requesting "all of them" must still get a bounded response, not an unbounded table dump.
     */
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentTransactionService paymentTransactionService;

    @Override
    public WsResponse<?> listPaymentTransactions(int page, int size) {
        int clampedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<PaymentTransaction> result = paymentTransactionService.findAll(PageRequest.of(page, clampedSize));

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
