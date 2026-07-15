package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Respuesta interna para ejecuciones transaccionales.
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class ExecuteResponse extends BaseTransactionResponse {
    @Schema(example = "1")
    private String chain;

    @Schema(example = "148")
    private String store;

    @JsonProperty("store_name")
    @Schema(example = "FYBECA AMAZONAS")
    private String storeName;

    @Schema(example = "1")
    private String pos;

    @JsonProperty("channel_POS")
    @Schema(example = "POS")
    private String channelPos;

    @JsonProperty("category_code")
    @Schema(example = "1")
    private String categoryCode;

    @JsonProperty("subcategory_code")
    @Schema(example = "1")
    private String subcategoryCode;

    @JsonProperty("service_provider_code")
    @Schema(example = "1")
    private String serviceProviderCode;

    @JsonProperty("rms_item_code")
    @Schema(example = "100713841")
    private String rmsItemCode;

    @Schema(example = "Carlos")
    private String username;

    @Schema(example = "Perez")
    private String lastname;

    @Schema(example = "USD")
    private String currency;

    @Schema(example = "91081")
    private String authorization;

    @Schema(example = "SN001-XYZ")
    private String serialnumber;

    @Schema(example = "997561")
    private String userid;

    @Schema(example = "0912345678")
    private String document;

    @Schema(example = "25.50")
    private BigDecimal amount;

    @JsonProperty("boleto_clave")
    @Schema(example = "853213656545", description = "Clave del boleto (solo Tradicionales)")
    private String boletoClave;

    @JsonProperty("boleto_qr")
    @Schema(example = "https://...", description = "URL QR del boleto (solo Tradicionales)")
    private String boletoQr;

    @JsonProperty("fecha_venta")
    @Schema(example = "2025-04-17", description = "Fecha de venta (solo Tradicionales)")
    private String fechaVenta;

    @JsonProperty("fracciones_vendidas")
    @Schema(example = "05,06,07,11,12,15,17,21,23,24",
            description = "CSV de las fracciones especificas asignadas por el proveedor a este boleto (solo La Loteria) "
                    + "— para Pozo Millonario, la combinacion secundaria (ver fracciones_vendidas_detalle para el detalle completo)")
    private String fraccionesVendidas;

    @JsonProperty("boleto_id")
    @Schema(example = "508085", description = "Id de boleto — liga Pozo Millonario con su Revancha cuando comparten valor (solo Tradicionales)")
    private String boletoId;

    @JsonProperty("valor_total_vendido")
    @Schema(example = "1.00", description = "Valor total vendido para este boleto segun el proveedor (solo Tradicionales)")
    private BigDecimal valorTotalVendido;

    @JsonProperty("fracciones_vendidas_detalle")
    @Schema(description = "Detalle de cada fraccion/combinacion vendida en este boleto — para Pozo Millonario "
            + "el segundo registro es la mascota seleccionada (solo Tradicionales)")
    private List<SoldFraction> fraccionesVendidasDetalle;

    @JsonProperty("pci")
    @Schema(example = "999", description = "Id del Pago de premios de canales, id interno del proveedor (solo CASH_OUT Tradicionales)")
    private String pci;

    @JsonProperty("ref")
    @Schema(example = "473856", description = "Id de referencia interna del pago de premio (solo CASH_OUT Tradicionales)")
    private String ref;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SoldFraction {
        @JsonProperty("numero_fraccion")
        @Schema(example = "02,06,10,12,13,17,19,20,21,23,24")
        private String numeroFraccion;

        @JsonProperty("nombre_combinacion")
        @Schema(example = "Mascota 10 (Gato)")
        private String nombreCombinacion;

        @JsonProperty("valor_premio")
        private BigDecimal valorPremio;
    }
}
