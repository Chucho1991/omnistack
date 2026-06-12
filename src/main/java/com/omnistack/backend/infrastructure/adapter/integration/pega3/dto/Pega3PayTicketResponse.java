package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de pago de ticket Pega3 (PagarTicket - EXECUTE).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3PayTicketResponse {
    private String gameTicketNumber;
    private BigDecimal totalClaimedAmount;
    private String claimedOn;
    private String message;
}
