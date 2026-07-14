package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar si un boleto de Tradicionales tiene premio (ConsultarTicket - PRECHECK CASH_OUT).
 */
@Value
@Builder
public class TradicionalConsultarTicketCommand {
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
    String userId;
    String clienteId;
    String mor;
    Integer tipoDocumento;
    String numeroDocumento;
    String nombreGanador;
    String cl;
    String cb;
    String co;
    Boolean premioEspecie;
}
