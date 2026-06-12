package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para crear un ticket de apuesta Pega3 (CrearTicket).
 */
@Value
@Builder
public class Pega3CreateTicketCommand {
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
    BigDecimal amount;
    Integer drawNumber;
    String entryType;
    List<Pega3Panel> panels;
}
