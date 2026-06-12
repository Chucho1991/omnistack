package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar juegos disponibles (RecuperarJuegosPorMedio - PRECHECK step 1).
 */
@Value
@Builder
public class TradicionalJuegoQueryCommand {
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
}
