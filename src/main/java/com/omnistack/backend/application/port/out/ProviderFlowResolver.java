package com.omnistack.backend.application.port.out;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ProviderFlowSelection;

/**
 * Resuelve el flujo adecuado segun catalogo y capacidad solicitada.
 */
public interface ProviderFlowResolver {

    /**
     * Resuelve la estrategia de proveedor para una capacidad solicitada.
     *
     * @param request request transaccional recibido
     * @param capability capacidad requerida
     * @return seleccion de servicio y estrategia
     */
    ProviderFlowSelection resolve(BaseTransactionRequest request, Capability capability);
}
