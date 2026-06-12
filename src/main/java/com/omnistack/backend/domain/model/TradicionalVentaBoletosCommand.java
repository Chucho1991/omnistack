package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para vender boletos (VentaBoletos - EXECUTE).
 */
@Value
@Builder
public class TradicionalVentaBoletosCommand {
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
    String cliente;
    String ordenCompra;
    String reservaId;
    BigDecimal totalVenta;
    String formaCobro;
    String numeroIdentificacion;
    String nombreComprador;
    String numeroCelularComprador;
    String correoElectronicoComprador;
    String juegoId;
    String sorteoId;
    String numero;
    Integer cantidadBoletos;
}
