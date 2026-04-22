package com.omnistack.backend.domain.model;

import com.omnistack.backend.application.port.out.strategy.TransactionFlowStrategy;
import lombok.Builder;
import lombok.Value;

/**
 * Resultado de resolucion del flujo para un proveedor y capacidad.
 */
@Value
@Builder
public class ProviderFlowSelection {
    ServiceDefinition serviceDefinition;
    TransactionFlowStrategy strategy;
}
