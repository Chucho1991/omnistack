package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para reversar recargas BET593 en Loteria Nacional.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bet593RechargeReverseRequest {
    String usuario;
    String maquina;
    String operacion;
    String token;
    String usuarioId;
    Integer medioId;
    Integer clienteId;
    String numeroTransaccion;
    String identificacion;
    String motivo;
}
