package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para pagar el premio de un boleto de Tradicionales (PagoPremioTicketTradicional - EXECUTE CASH_OUT).
 */
@Value
@Builder
public class TradicionalPagoPremioCommand {
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
    String mpi;
}
