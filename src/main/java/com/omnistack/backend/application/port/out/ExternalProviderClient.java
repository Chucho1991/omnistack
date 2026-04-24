package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionRequest;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Cliente desacoplado para consumo de proveedores externos.
 */
public interface ExternalProviderClient {

    ExternalTransactionResponse invoke(ExternalTransactionRequest request);
}
