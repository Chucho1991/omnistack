package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de consulta de ticket Pega3 (ConsultarTicket - VERIFY).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3VerifyTicketResponse {
    private String ticketNumber;
    private String gameTicketNumber;
    private String status;
    private Boolean isWinner;
    private BigDecimal prizeAmount;
    private String message;
}
