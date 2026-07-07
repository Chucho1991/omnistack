package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.shared.validation.ValidTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO de entrada para ejecutar una transaccion en proveedor externo.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidTransactionRequest
@Schema(description = "Solicitud de ejecucion transaccional")
public class ExecuteRequest extends BaseTransactionRequest {
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "25.50")
    private BigDecimal amount;

    @Schema(example = "Ariel Castillo", description = "Nombre del comprador (solo Tradicionales)")
    private String username;

    @JsonProperty("boleto_data")
    @Schema(description = "Datos del boleto a comprar (solo Tradicionales, un unico boleto). Ignorado si se envia lista_boletos.")
    private BoletoData boletoData;

    @JsonProperty("lista_boletos")
    @Schema(description = "Carrito de jugadas: multiples boletos/juegos en una misma venta (solo Tradicionales). Si viene, tiene prioridad sobre boleto_data.")
    private List<BoletoData> listaBoletos;

    @JsonProperty("reserva_id")
    @Schema(example = "535852", description = "Id de la pre-reserva devuelto por CREATE_TICKET (reserva_id) — "
            + "obligatorio en Tradicionales para vender los numeros reservados")
    private String reservaId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoletoData {
        @JsonProperty("game_id")
        @Schema(example = "1")
        private String gameId;

        @JsonProperty("draw_id")
        @Schema(example = "7210")
        private String drawId;

        @Schema(example = "20911")
        private String numero;

        @JsonProperty("cantidad_boletos")
        @Schema(example = "1")
        private Integer cantidadBoletos;
    }
}
