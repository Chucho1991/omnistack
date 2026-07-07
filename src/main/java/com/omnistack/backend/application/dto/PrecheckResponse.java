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

        @JsonProperty("max_cost")
        @Schema(example = "30.00", description = "Monto maximo de apuesta (maxWager del proveedor)")
        private BigDecimal maxCost;

        @JsonProperty("future_draws_limit")
        @Schema(example = "12", description = "Ventana maxima de sorteos futuros para reserva")
        private Integer futureDrawsLimit;

        @JsonProperty("advance_draw_limit")
        @Schema(example = "1", description = "Cantidad de sorteos futuros que se pueden saltar")
        private Integer advanceDrawLimit;

        @JsonProperty("play_types")
        @Schema(example = "[\"Straight\", \"Box\"]")
        private List<String> playTypes;

        @JsonProperty("prize_liability_threshold")
        @Schema(example = "5000.00", description = "Monto de premio a partir del cual el proveedor exige control de riesgo antes de continuar (RN-07)")
        private BigDecimal prizeLiabilityThreshold;
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

    @JsonProperty("draws")
    @Schema(description = "Lista de sorteos disponibles (solo Tradicionales)")
    private List<TradicionalDraw> draws;

    @JsonProperty("figures")
    @Schema(description = "Lista de figuras del juego (solo Tradicionales — Loteria)")
    private List<TradicionalFigura> figures;

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

        @JsonProperty("cantidad_fraccion")
        @Schema(description = "Cantidad de fracciones por entero (solo La Loteria; 0 o null en juegos sin fraccion)")
        private Integer cantidadFraccion;

        @JsonProperty("tiene_revancha")
        @Schema(description = "True si el sorteo tiene un Pozo Revancha asociado (solo Pozo Millonario)")
        private Boolean tieneRevancha;

        @JsonProperty("juego_revancha_id")
        @Schema(example = "17", description = "juegoId del Pozo Revancha asociado")
        private String juegoRevanchaId;

        @JsonProperty("sorteo_revancha_id")
        private String sorteoRevanchaId;
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
}
