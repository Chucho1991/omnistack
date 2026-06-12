package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para generar comprobante de venta (GenerarComprobanteVenta - VERIFY).
 */
@Value
@Builder
public class TradicionalVerifyCommand {
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
    String ventaId;
    String idUsuario;
    String puntoDeVenta;
}
