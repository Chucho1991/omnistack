package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para pagar ticket Pega3 (PagarTicket - EXECUTE).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3PayTicketRequest {
    String deviceId;
    String token;
    String customerSessionId;
    String productoVender;
    String ticketNumber;
    BigDecimal amount;
}
