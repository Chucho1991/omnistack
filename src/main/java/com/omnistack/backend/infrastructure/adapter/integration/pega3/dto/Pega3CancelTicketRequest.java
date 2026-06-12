package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para cancelar ticket Pega3 (CancelarTicket - REVERSE).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3CancelTicketRequest {
    String deviceId;
    String token;
    String productoVender;
    String ticketNumber;
    String customerSessionId;
}
