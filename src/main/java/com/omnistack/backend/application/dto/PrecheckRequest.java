package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.shared.validation.ValidTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO de entrada para la operacion de prevalidacion transaccional.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidTransactionRequest
@Schema(description = "Solicitud de validacion previa")
public class PrecheckRequest extends BaseTransactionRequest {
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "25.50")
    private BigDecimal amount;

    @JsonProperty("game_id")
    @Schema(example = "1", description = "ID de juego (solo Tradicionales)")
    private String gameId;

    @JsonProperty("draw_id")
    @Schema(example = "7210", description = "ID de sorteo (solo Tradicionales)")
    private String drawId;

    @Schema(example = "20911", description = "Combinacion de numeros para consulta (solo Tradicionales)")
    private String combinacion;

    @Schema(example = "false", description = "Sugerir numeros disponibles (solo Tradicionales)")
    private Boolean sugerir;

    @Schema(example = "10", description = "Cantidad de registros a retornar (solo Tradicionales)")
    private Integer registros;
}
