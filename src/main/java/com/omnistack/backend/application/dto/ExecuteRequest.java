package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonProperty("mpi")
    @Schema(example = "957", description = "Id del movimiento de pago devuelto por el PRECHECK de CASH_OUT (solo Tradicionales) — "
            + "el POS lo reenvia tal cual, no lo digita el cajero")
    private String mpi;

    @JsonProperty("tipo_documento")
    @Schema(example = "2", description = "Tipo de documento del ganador: 1-RUC, 2-Cedula, 3-Pasaporte (solo CASH_OUT Tradicionales) — "
            + "el POS lo reenvia del PRECHECK")
    private Integer tipoDocumento;

    @jakarta.validation.Valid
    @JsonProperty("ticket_data")
    @JsonAlias("ticketData")
    @Schema(description = "Datos del ticket a crear y vender (solo Pega3) — CrearTicket vende el ticket "
            + "por completo, no hay paso de CREATE_TICKET separado para este proveedor")
    private TicketData ticketData;

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

    /**
     * Datos del juego para el ticket a crear (solo Pega3).
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
        @Schema(example = "Verbal-Manual")
        private String entryType;

        @jakarta.validation.Valid
        @jakarta.validation.constraints.NotEmpty
        @Schema(description = "Paneles de apuesta")
        private List<TicketPanel> panels;
    }

    /**
     * Panel individual de apuesta dentro del ticket (solo Pega3).
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

        @Schema(example = "[1, 2, 3]")
        private List<Integer> numbers;

        @JsonProperty("play_types")
        @JsonAlias("playTypes")
        @Schema(example = "[\"WIN\"]")
        private List<String> playTypes;
    }
}
