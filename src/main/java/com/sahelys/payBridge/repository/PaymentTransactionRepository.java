package com.sahelys.payBridge.repository;

import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByClientPaymentRequestRef(UUID clientPaymentRequestRef);

    /**
     * Both bounds are always concrete, non-null values -- see
     * {@link com.sahelys.payBridge.services.PaymentTransactionService#findAll} for why: a
     * {@code (:start IS NULL OR ...)} pattern here makes Postgres's JDBC driver unable to infer
     * the bind parameter's type ("could not determine data type of parameter $1").
     */
    Page<PaymentTransaction> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);
}
