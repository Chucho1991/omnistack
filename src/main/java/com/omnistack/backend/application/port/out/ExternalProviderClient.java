package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionRequest;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Cliente desacoplado para consumo de proveedores externos.
 */
public interface ExternalProviderClient {

    /**
     * Invoca un proveedor externo con un request canonico.
     *
     * @param request solicitud externa canonica
     * @return respuesta canonica del proveedor
     */
    ExternalTransactionResponse invoke(ExternalTransactionRequest request);
}
