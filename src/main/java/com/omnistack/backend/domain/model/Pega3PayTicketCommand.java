package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para pagar un ticket Pega3 (PagarTicket - EXECUTE).
 */
@Value
@Builder
public class Pega3PayTicketCommand {
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
    String ticketNumber;
}
