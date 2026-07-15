package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de creacion de ticket Pega3 (CrearTicket).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3CreateTicketResponse {
    private String ticketNumber;
    private String gameTicketNumber;
    private BigDecimal cost;
    private String drawDate;
    private String status;
    private String message;

    /** URL del codigo QR del ticket vendido — disponible de inmediato en CrearTicket, a
     * diferencia de ConsultarTicket (VERIFY) que no lo trae. Es el dato imprimible principal;
     * GenerarComprobantePega es solo para reimpresion/fallback. */
    private String codigoQR;
}
