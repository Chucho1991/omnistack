package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo del login requerido por los servicios de Loteria.
 */
@Value
@Builder
public class LoteriaLoginRequest {
    String username;
    String password;
    @JsonProperty("productoVender")
    String productToSell;
}
