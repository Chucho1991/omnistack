package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para nota de retiro BET593 en Loteria Nacional.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bet593WithdrawRequest {
    String usuario;
    String maquina;
    String operacion;
    String token;
    String usuarioId;
    Integer clienteId;
    Integer medioId;
    String numeroTransaccion;
    String identificacion;
    String numeroRetiro;
}
