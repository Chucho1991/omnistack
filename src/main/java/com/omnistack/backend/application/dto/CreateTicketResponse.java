package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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

    @JsonProperty("ticket_qr")
    @Schema(example = "https://www8.loteria.com.ec/LNConsultaBoletos/GanadoresLN?cn=TO0119007180211600081357150",
            description = "URL del codigo QR del ticket, para imprimir en el comprobante (solo Pega3) — "
                    + "disponible de inmediato aqui, no requiere VERIFY")
    private String ticketQr;

    @JsonProperty("available_numbers")
    @Schema(description = "Combinaciones encontradas/reservadas (solo Tradicionales)")
    private List<TradicionalNumber> availableNumbers;

    @JsonProperty("total_numbers")
    @Schema(description = "Total de combinaciones encontradas (solo Tradicionales)")
    private Integer totalNumbers;

    @JsonProperty("reserva_id")
    @Schema(description = "Id de la pre-reserva generada por el proveedor (solo Tradicionales) — "
            + "debe reenviarse tal cual en EXECUTE para vender los numeros aqui reservados")
    private String reservaId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TradicionalNumber {
        private String numero;

        @Schema(description = "Segunda parte de la combinacion cuando el juego la usa (ej. Pozo Millonario: "
                + "digitos de la combinacion secundaria/mascota) — null si el juego no la tiene")
        private String numero2;
        private String numero3;
        private String numero4;
        private String numero5;

        private String figura;

        @JsonProperty("game_id")
        private String juegoId;

        @JsonProperty("draw_id")
        private String sorteoId;

        /** Id de boleto — asocia Pozo Millonario con su Revancha cuando comparten valor. */
        private String boleto;

        @JsonProperty("fracciones")
        private String fracciones;

        @Schema(description = "Cantidad de fracciones/unidades solicitadas en la busqueda")
        private String cantidad;

        @Schema(description = "Cantidad de fracciones/unidades que el proveedor realmente reservo para este "
                + "numero — puede diferir de lo solicitado")
        private String reserva;

        @JsonProperty("precio_unitario")
        @Schema(example = "1.00", description = "Precio por unidad/fraccion (pvp) del sorteo al que pertenece "
                + "este numero — NO es un total, el front debe multiplicarlo por la cantidad de fracciones "
                + "que el cajero realmente seleccione/compre.")
        private java.math.BigDecimal precioUnitario;
    }
}
