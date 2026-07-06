package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para generar el comprobante PDF de una venta Pega3 (GenerarComprobantePega - VERIFY).
 */
@Value
@Builder
public class Pega3ComprobanteQueryCommand {
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
    String transaccion;
    String puntoDeVenta;
}
