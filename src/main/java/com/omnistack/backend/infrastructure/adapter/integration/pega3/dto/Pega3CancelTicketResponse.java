package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de cancelacion de ticket Pega3 (CancelarTicket - REVERSE).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3CancelTicketResponse {
    private String gameTicketNumber;
    private BigDecimal refundedAmount;
    private String canceledOn;
    private String message;
}
