package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Request externo para recarga de saldo BET593 en Loteria Nacional.
 */
@Value
@Builder
public class Bet593RechargeRequest {
    String usuario;
    String token;
    String canal;
    Integer medioId;
    Integer puntooperacionId;
    String cuentaweb;
    String valor;
    String codigotrn;
}
