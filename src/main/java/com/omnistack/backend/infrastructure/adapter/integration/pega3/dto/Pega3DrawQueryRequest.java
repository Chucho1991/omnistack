package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para consultar sorteo activo Pega3 (ObtieneSorteosActivoxJuego).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3DrawQueryRequest {
    String deviceId;
    String token;
    String productoVender;
}
