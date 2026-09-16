package com.sahelys.payBridge.repository;

import com.sahelys.payBridge.domain.entities.OperatorCallback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OperatorCallbackRepository extends JpaRepository<OperatorCallback, UUID> {
}
