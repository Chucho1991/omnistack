package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradicionalAnularVentaRequest {
    String userName;
    String token;
    String medioId;
    String clienteId;
    String ordenCompra;
    String motivo;
}
