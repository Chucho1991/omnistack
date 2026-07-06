package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para vender boletos (VentaBoletos - EXECUTE). Soporta carrito:
 * un boleto (venta simple) o varios boletos/juegos en la misma venta.
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
    List<BoletoEntry> boletos;

    @Value
    @Builder
    public static class BoletoEntry {
        String juegoId;
        String sorteoId;
        String numero;
        Integer cantidadBoletos;
    }
}
