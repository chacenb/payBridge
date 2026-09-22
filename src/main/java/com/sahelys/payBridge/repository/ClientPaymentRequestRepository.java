package com.sahelys.payBridge.repository;

import com.sahelys.payBridge.domain.entities.ClientPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientPaymentRequestRepository extends JpaRepository<ClientPaymentRequest, UUID> {

    Optional<ClientPaymentRequest> findByClientAppIdAndClientPaymentRequestId(
            String clientAppId, String clientPaymentRequestId);
}
