package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar el producto Pega3 (VentaProductos).
 */
@Value
@Builder
public class Pega3ProductQueryCommand {
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
