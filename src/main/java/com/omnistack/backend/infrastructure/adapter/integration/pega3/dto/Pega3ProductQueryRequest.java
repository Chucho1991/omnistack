package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para consultar producto Pega3 (VentaProductos).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3ProductQueryRequest {
    String token;
    String productoVender;
}
