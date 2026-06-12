package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para consultar ticket Pega3 (ConsultarTicket - VERIFY).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3VerifyTicketRequest {
    String token;
    String productoVender;
    String ticketNumber;
    String customerSessionId;
}
