package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
}
