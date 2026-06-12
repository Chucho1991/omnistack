package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar el estado de un ticket Pega3 (ConsultarTicket - VERIFY).
 */
@Value
@Builder
public class Pega3VerifyTicketCommand {
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
}
