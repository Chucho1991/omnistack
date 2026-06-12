package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar el sorteo activo Pega3 (ObtieneSorteosActivo).
 */
@Value
@Builder
public class Pega3DrawQueryCommand {
    String uuid;
    String chain;
    String store;
    String pos;
    String channelPos;
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String rmsItemCode;
}
