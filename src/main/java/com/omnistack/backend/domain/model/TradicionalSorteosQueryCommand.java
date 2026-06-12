package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar sorteos por juego (RecuperarSorteosDisponibles - PRECHECK step 2).
 */
@Value
@Builder
public class TradicionalSorteosQueryCommand {
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
    Integer medioId;
    String userName;
    String juegoId;
}
