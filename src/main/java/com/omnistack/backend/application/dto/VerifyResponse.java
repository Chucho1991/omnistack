package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * DTO de salida para la verificacion posterior de transacciones.
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class VerifyResponse extends BaseTransactionResponse {
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
    @Schema(example = "2")
    private String serviceProviderCode;

    @JsonProperty("rms_item_code")
    @Schema(example = "100708850")
    private String rmsItemCode;

    @Schema(example = "Carlos")
    private String username;

    @Schema(example = "Perez")
    private String lastname;

    @Schema(example = "USD")
    private String currency;

    @Schema(example = "9F968187-F436-4F19-8C1F-A7A4DA07A899")
    private String authorization;

    @Schema(example = "7366ea56284a06a2a58f561b497386b80fcd3eaea858d0c511")
    private String serialnumber;

    @Schema(example = "997561")
    private String userid;

    @Schema(example = "0901111112")
    private String document;

    @JsonProperty("ticket_number")
    @Schema(example = "TICKET-12345", description = "Numero de ticket (solo Pega3)")
    private String ticketNumber;

    @JsonProperty("ticket_status")
    @Schema(example = "PAID", description = "Estado del ticket (solo Pega3)")
    private String ticketStatus;

    @JsonProperty("is_winner")
    @Schema(example = "false", description = "Indica si el ticket es ganador (solo Pega3)")
    private Boolean winner;

    @JsonProperty("prize_amount")
    @Schema(example = "0.00", description = "Monto del premio (solo Pega3)")
    private BigDecimal prizeAmount;

    @JsonProperty("comprobante_urls")
    @Schema(description = "URLs para descargar el/los comprobante(s) de venta (solo Tradicionales y Pega3) — "
            + "una entrada por cada boleto/juego vendido en la misma venta (ej. Pozo Millonario + Revancha "
            + "generan 2 comprobantes separados)")
    private List<String> comprobanteUrls;
}
