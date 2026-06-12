package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar figuras por juego (RecuperarFigurasPorJuego - PRECHECK step 3).
 */
@Value
@Builder
public class TradicionalFigurasQueryCommand {
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
