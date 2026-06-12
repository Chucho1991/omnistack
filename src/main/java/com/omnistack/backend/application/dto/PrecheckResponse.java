package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
 * DTO de salida para la respuesta de prevalidacion.
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrecheckResponse extends BaseTransactionResponse {
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

    @Schema(example = "AUTO-1234567890")
    private String authorization;

    @Schema(example = "SN-001")
    private String serialnumber;

    @Schema(example = "997561")
    private String userid;

    @Schema(example = "0912345678")
    private String document;

    @Schema(example = "25.50")
    private BigDecimal amount;

    @JsonProperty("game_data")
    @Schema(description = "Datos del juego disponibles (solo Pega3)")
    private GameData gameData;

    @JsonProperty("active_draw")
    @Schema(description = "Sorteo activo disponible (solo Pega3)")
    private ActiveDraw activeDraw;

    /**
     * Informacion del producto de juego retornada por VentaProductos (Pega3).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GameData {
        @JsonProperty("entry_types")
        @Schema(example = "[\"QUICK_PICK\", \"SELF_PICK\"]")
        private List<String> entryTypes;

        @JsonProperty("bet_amount_options")
        @Schema(example = "[1.0, 2.0, 5.0]")
        private List<BigDecimal> betAmountOptions;

        @JsonProperty("min_cost")
        @Schema(example = "1.00")
        private BigDecimal minCost;
    }

    /**
     * Sorteo activo retornado por ObtieneSorteosActivo (Pega3).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveDraw {
        @JsonProperty("draw_number")
        @Schema(example = "1234")
        private Integer drawNumber;

        @JsonProperty("draw_date")
        @Schema(example = "2026-05-14T19:00:00")
        private String drawDate;
    }

    @JsonProperty("games")
    @Schema(description = "Lista de juegos disponibles (solo Tradicionales)")
    private List<TradicionalGame> games;

    @JsonProperty("draws")
    @Schema(description = "Lista de sorteos disponibles (solo Tradicionales)")
    private List<TradicionalDraw> draws;

    @JsonProperty("figures")
    @Schema(description = "Lista de figuras del juego (solo Tradicionales — Loteria)")
    private List<TradicionalFigura> figures;

    @JsonProperty("available_numbers")
    @Schema(description = "Lista de numeros disponibles para el sorteo (solo Tradicionales)")
    private List<TradicionalNumber> availableNumbers;

    @JsonProperty("total_numbers")
    @Schema(description = "Total de numeros disponibles (solo Tradicionales)")
    private Integer totalNumbers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalGame {
        @JsonProperty("game_id")
        private String gameId;
        private String nombre;
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalDraw {
        @JsonProperty("draw_id")
        private String drawId;
        private String nombre;
        private String fecha;
        private java.math.BigDecimal precio;
        @JsonProperty("premio_mayor")
        private java.math.BigDecimal premioMayor;
        private Boolean disponible;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalFigura {
        @JsonProperty("figura_id")
        private String figuraId;
        private String nombre;
        private String descripcion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalNumber {
        private String numero;
        private Boolean disponible;
        private java.math.BigDecimal precio;
    }
}
