package com.omnistack.backend.application.port.out.strategy;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ServiceDefinition;

/**
 * Estrategia base para una capacidad transaccional.
 */
public interface TransactionFlowStrategy {

    /**
     * Indica si la estrategia soporta un servicio y capacidad.
     *
     * @param serviceDefinition definicion comercial del servicio
     * @param capability capacidad solicitada
     * @return true si la estrategia puede procesar el flujo
     */
    boolean supports(ServiceDefinition serviceDefinition, Capability capability);

    /**
     * Procesa el flujo transaccional para la capacidad resuelta.
     *
     * @param request request transaccional interno
     * @param serviceDefinition definicion comercial del servicio
     * @param capability capacidad solicitada
     * @return respuesta transaccional interna
     */
    BaseTransactionResponse process(BaseTransactionRequest request, ServiceDefinition serviceDefinition, Capability capability);
}
