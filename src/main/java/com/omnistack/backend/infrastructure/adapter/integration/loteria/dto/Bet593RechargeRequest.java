package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para recarga de saldo BET593 en Loteria Nacional.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Bet593RechargeRequest {
    String usuario;
    String token;
    String canal;
    Integer medioId;
    Integer puntooperacionId;
    String cuentaweb;
    String recargaid;
    String serialnumber;
    String valor;
    String codigotrn;
}
