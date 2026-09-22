package com.sahelys.payBridge.repository;

import com.sahelys.payBridge.domain.entities.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    Optional<PaymentTransaction> findByClientPaymentRequestRef(UUID clientPaymentRequestRef);
}
