package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * El proveedor real en QA responde con nombres distintos a los que documenta el .docx
 * (listaVentaSuerte/listaVentaSorteos/listaNumerosVendidos en vez de
 * listaSUE/listaSorteos/ListaR) — se mapea a lo que efectivamente llega, con @JsonAlias
 * a los nombres documentados por si algun ambiente todavia los usa.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalVentaBoletosResponse {
    private Object codError;
    private String msgError;
    private String usuario;
    private String transaccion;
    private String token;
    private String ventaId;
    private String fechaVenta;

    @JsonProperty("listaVentaSuerte")
    @JsonAlias("listaSUE")
    private List<Sue> listaSUE;

    public boolean isSuccess() {
        return "0".equals(String.valueOf(codError));
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sue {
        private String numeroComprobanteSuerte;
        private String anuncio1;
        private String anuncio2;
        private String anuncio3;

        @JsonProperty("listaVentaSorteos")
        @JsonAlias("listaSorteos")
        private List<SorteoDetalle> listaSorteos;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SorteoDetalle {
        private String juegoId;
        private String sorteoId;
        private String nombreJuego;
        private String nombreSorteo;
        private String fechaSorteo;
        private String fechaCaducidad;
        private BigDecimal precioVentaPublico;
        private BigDecimal premioMayorPrimeraSuerte;

        /** DIN (dinero) / ESP (especies). */
        private String tipoPremioPrimeraSuerte;
        private String nombrePremioPrimeraSuerte;
        private Integer cantidadFracciones;

        /** Clase de sorteo: O - Ordinario / E - Extraordinario. */
        private String clase;

        @JsonProperty("listaNumerosVendidos")
        @JsonAlias("ListaR")
        private List<TicketDetalle> listaR;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TicketDetalle {
        private String clave;
        private String numero;

        /** Id de boleto — liga Pozo Millonario con su Revancha cuando comparten valor. */
        private String boleto;

        private BigDecimal valorTotalNumeroVendido;
        private Integer cantidad;
        private BigDecimal valor;
        private String codigoQR;

        /**
         * Fracciones especificas asignadas por el proveedor a este boleto (solo La Loteria).
         * Para Pozo Millonario el proveedor manda 2 registros: [0] = combinacion secundaria
         * (ej. "Combinacion 11/25 (Pozo Millonario)"), [1] = mascota seleccionada
         * (ej. "Mascota 10 (Gato)").
         */
        private List<NumeroFraccion> listaNumeroFracciones;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NumeroFraccion {
        private String numeroFraccion;
        private String nombreCombinacion;
        private BigDecimal valorPremio;
    }
}
