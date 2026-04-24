package com.omnistack.backend.application.port.out;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ProviderFlowSelection;

/**
 * Resuelve el flujo adecuado segun catalogo y capacidad solicitada.
 */
public interface ProviderFlowResolver {

    ProviderFlowSelection resolve(BaseTransactionRequest request, Capability capability);
}
