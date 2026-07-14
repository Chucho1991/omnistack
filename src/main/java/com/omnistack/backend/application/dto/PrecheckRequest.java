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
    @Schema(example = "1", description = "ID de juego (solo Tradicionales). Opcional — si no se envia, se deriva del rms_item_code.")
    private String gameId;

    @JsonProperty("tipo_documento")
    @Schema(example = "2", description = "Tipo de documento del ganador: 1-RUC, 2-Cedula, 3-Pasaporte (solo CASH_OUT Tradicionales, boleto electronico)")
    private Integer tipoDocumento;

    @JsonProperty("nombre_ganador")
    @Schema(example = "Ariel Castillo", description = "Nombre del ganador (opcional, solo CASH_OUT Tradicionales)")
    private String nombreGanador;
}
