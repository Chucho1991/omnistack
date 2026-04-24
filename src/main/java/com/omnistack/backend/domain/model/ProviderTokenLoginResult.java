package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Resultado interno del proceso de autenticacion contra un proveedor externo.
 */
@Value
@Builder
public class ProviderTokenLoginResult {
    String token;
}
