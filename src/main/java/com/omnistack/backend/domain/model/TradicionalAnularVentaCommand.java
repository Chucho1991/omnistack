package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para anular una venta (AnularVentaBoletos - REVERSE).
 */
@Value
@Builder
public class TradicionalAnularVentaCommand {
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
    String clienteId;
    String ordenCompra;
    String motivo;
}
