package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

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
    private List<Sue> listaSUE;

    public boolean isSuccess() {
        return "0".equals(String.valueOf(codError));
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sue {
        private String anuncio1;
        private String anuncio2;
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

        @JsonProperty("ListaR")
        private List<TicketDetalle> listaR;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TicketDetalle {
        private String clave;
        private String numero;
        private Integer cantidad;
        private BigDecimal valor;
        private String codigoQR;

        /**
         * Fracciones especificas asignadas por el proveedor a este boleto (solo La Loteria).
         * El proveedor documenta que el segundo registro de este arreglo puede traer el
         * codigo de mascota en vez de otra fraccion (solo aplica a Pozo Millonario).
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
