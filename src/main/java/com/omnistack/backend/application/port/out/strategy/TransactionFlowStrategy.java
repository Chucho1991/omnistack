package com.omnistack.backend.application.port.out.strategy;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ServiceDefinition;

/**
 * Estrategia base para una capacidad transaccional.
 */
public interface TransactionFlowStrategy {

    boolean supports(ServiceDefinition serviceDefinition, Capability capability);

    BaseTransactionResponse process(BaseTransactionRequest request, ServiceDefinition serviceDefinition, Capability capability);
}
