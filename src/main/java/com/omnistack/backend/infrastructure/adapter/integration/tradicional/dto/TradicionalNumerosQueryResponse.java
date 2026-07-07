package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalNumerosQueryResponse {
    private Object codError;
    private String msgError;
    private Integer totalResults;

    /** Id de la pre-reserva que crea el proveedor en esta consulta; debe reenviarse tal cual
     * en VentaBoletos.reservaId para vender los numeros aqui reservados. */
    private String numeroReserva;

    @JsonProperty("listaDetalle")
    private List<Numero> listaNumeros;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Numero {
        private String numero;
        private String numero2;
        private String numero3;
        private String numero4;
        private String numero5;

        /** Cantidad de fracciones solicitadas (viene como numero o string segun el juego). */
        private String cantidad;

        /** Codigo de la figura asociada al sorteo (mascota/fruta), distinto de combinacionFigura del request. */
        private String figura;

        @JsonProperty("Id")
        private String id;

        private String juegoId;
        private String sorteoId;

        /** CSV de fracciones disponibles para este numero, ej "1,2,3,4,5,6,7,8,9,10". Solo aplica a La Loteria. */
        private String fracciones;

        /** Id de boleto — asocia Pozo Millonario (juegoId 5) con su Revancha (juegoId 17) cuando coincide. */
        private String boleto;

        /** Cantidad de fracciones reservadas. */
        private String reserva;
    }
}
