package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para ConsultarTicket (Tradicionales - PRECHECK CASH_OUT).
 */
@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TradicionalConsultarTicketRequest {
    String token;
    String productoVender;
    String userId;
    String clienteId;
    @JsonProperty("MOR")
    String mor;
    Integer tipoDocumento;
    String numeroDocumento;
    String nombreGanador;
    String cl;
    String cb;
    String co;
    Boolean premioEspecie;
}
