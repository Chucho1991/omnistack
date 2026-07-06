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
 * Respuesta interna para creacion de ticket (Pega3) o busqueda/reserva de
 * combinaciones (Tradicionales — RecuperarNumerosDisponiblesPorCombinacion,
 * que segun el proveedor "obtiene y reserva").
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public class CreateTicketResponse extends BaseTransactionResponse {

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
    @Schema(example = "2")
    private String subcategoryCode;

    @JsonProperty("service_provider_code")
    @Schema(example = "2")
    private String serviceProviderCode;

    @JsonProperty("rms_item_code")
    @Schema(example = "100713841")
    private String rmsItemCode;

    @Schema(example = "GAME-TICKET-12345", description = "gameTicketNumber del proveedor Pega3")
    private String authorization;

    @JsonProperty("ticket_number")
    @Schema(example = "TICKET-12345", description = "ticketNumber del proveedor Pega3")
    private String ticketNumber;

    @JsonProperty("available_numbers")
    @Schema(description = "Combinaciones encontradas/reservadas (solo Tradicionales)")
    private List<TradicionalNumber> availableNumbers;

    @JsonProperty("total_numbers")
    @Schema(description = "Total de combinaciones encontradas (solo Tradicionales)")
    private Integer totalNumbers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalNumber {
        private String numero;
        private Boolean disponible;
        private BigDecimal precio;
        private String figura;

        @JsonProperty("juego_id")
        private String juegoId;

        @JsonProperty("sorteo_id")
        private String sorteoId;

        /** Id de boleto — asocia Pozo Millonario con su Revancha cuando comparten valor. */
        private String boleto;

        @JsonProperty("fracciones")
        private String fracciones;
    }
}
