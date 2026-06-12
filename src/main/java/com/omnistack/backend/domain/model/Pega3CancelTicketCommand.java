package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para cancelar un ticket Pega3 (CancelarTicket - REVERSE).
 */
@Value
@Builder
public class Pega3CancelTicketCommand {
    String uuid;
    String chain;
    String store;
    String storeName;
    String pos;
    String channelPos;
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String rmsItemCode;
    String ticketNumber;
    String motivo;
}
