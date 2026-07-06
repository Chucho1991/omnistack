package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.shared.validation.ValidTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
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
 * DTO de entrada para la operacion de creacion de ticket Pega3.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidTransactionRequest
@Schema(description = "Solicitud de creacion de ticket de apuesta")
public class CreateTicketRequest extends BaseTransactionRequest {

    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "5.00", description = "Requerido solo para Pega3 (Tradicionales no lo necesita en este paso)")
    private BigDecimal amount;

    @Valid
    @JsonProperty("ticket_data")
    @JsonAlias("ticketData")
    @Schema(description = "Datos del ticket a generar (solo Pega3)")
    private TicketData ticketData;

    @JsonProperty("draw_id")
    @Schema(example = "7151", description = "ID de sorteo, requerido (solo Tradicionales)")
    private String drawId;

    @Schema(example = "", description = "Combinacion de numeros a buscar/reservar, vacio = sin filtro (solo Tradicionales)")
    private String combinacion;

    @Schema(example = "false", description = "Sugerir combinaciones alternativas si no hay match exacto (solo Tradicionales)")
    private Boolean sugerir;

    @Schema(example = "10", description = "Cantidad de registros a retornar (solo Tradicionales)")
    private Integer registros;

    @JsonProperty("figura_id")
    @Schema(example = "01", description = "Codigo de mascota/fruta (solo Tradicionales)")
    private String figuraId;

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    /**
     * Datos del juego para el ticket a crear.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketData {

        @JsonProperty("draw_number")
        @JsonAlias("drawNumber")
        @Schema(example = "1234")
        private Integer drawNumber;

        @JsonProperty("entry_type")
        @JsonAlias("entryType")
        @Schema(example = "QUICK_PICK")
        private String entryType;

        @Valid
        @NotEmpty
        @Schema(description = "Paneles de apuesta")
        private List<TicketPanel> panels;
    }

    /**
     * Panel individual de apuesta dentro del ticket.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketPanel {

        @JsonProperty("bet_amount")
        @JsonAlias("betAmount")
        @Schema(example = "5.00")
        private BigDecimal betAmount;

        @Schema(example = "[1, 2, 3, 4, 5]")
        private List<Integer> numbers;

        @JsonProperty("play_types")
        @JsonAlias("playTypes")
        @Schema(example = "[\"WIN\"]")
        private List<String> playTypes;
    }
}
