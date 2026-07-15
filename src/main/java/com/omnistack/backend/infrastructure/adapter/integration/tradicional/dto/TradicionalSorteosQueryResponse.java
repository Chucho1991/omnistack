package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalSorteosQueryResponse {
    private Object codError;
    private String msgError;
    @JsonProperty("listaDetalle")
    private List<Sorteo> listaSorteos;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sorteo {
        private String juegoId;

        @JsonProperty("nombreJuego")
        private String nombreJuego;

        private String sorteoId;

        @JsonProperty("nombreSorteo")
        private String nombre;

        @JsonProperty("nombreSalaSorteo")
        private String nombreSalaSorteo;

        /** Clase de sorteo: O - Ordinario / E - Extraordinario. */
        private String clase;

        @JsonProperty("pvp")
        private BigDecimal precio;

        @JsonProperty("cantidadFraccion")
        private Integer cantidadFraccion;

        @JsonProperty("fechaSorteo")
        private String fecha;

        /** El spec documenta "fechaCierreVentas" (plural) pero el proveedor real en QA
         * manda "fechaCierreVenta" (singular) — se acepta cualquiera de los dos. */
        @JsonProperty("fechaCierreVenta")
        @JsonAlias("fechaCierreVentas")
        private String fechaCierreVentas;

        @JsonProperty("seAcumula")
        private Boolean seAcumula;

        /** String segun spec — solo trae valor cuando seAcumula=true. */
        @JsonProperty("montoProximoSorteo")
        private String montoProximoSorteo;

        @JsonProperty("valorPremio")
        private BigDecimal premioMayor;

        @JsonProperty("esSorteoDestacado")
        private Boolean esSorteoDestacado;

        /** Solo Pozo Millonario: etiqueta de cada "parte" adicional de la combinacion,
         * ej. "Combinacion 10/25 (Pozo Millonario)" / "Mascota 8 Pozo". */
        @JsonProperty("nombreSegundaCombinacion")
        private String nombreSegundaCombinacion;

        @JsonProperty("nombreTerceraCombinacion")
        private String nombreTerceraCombinacion;

        @JsonProperty("nombreCuartaCombinacion")
        private String nombreCuartaCombinacion;

        @JsonProperty("nombreQuintaCombinacion")
        private String nombreQuintaCombinacion;

        @JsonProperty("tienePremioInstantaneo")
        private Boolean tienePremioInstantaneo;

        /** DIN (dinero) / ESP (especies). */
        @JsonProperty("tipoPremioPrimeraSuerte")
        private String tipoPremioPrimeraSuerte;

        @JsonProperty("nombrePrimeraSuerte")
        private String nombrePrimeraSuerte;

        /** Cantidad de digitos que debe tener el numero/combinacion principal — el front
         * lo necesita para saber cuantos digitos pedir (Pozo Millonario). */
        @JsonProperty("cantidadDigitosCombinacionPrincipal")
        private Integer cantidadDigitosCombinacionPrincipal;

        @JsonProperty("cantidadDigitosCombinacionSecundaria")
        private Integer cantidadDigitosCombinacionSecundaria;

        @JsonProperty("tieneRevancha")
        private Boolean tieneRevancha;

        @JsonProperty("juegoRevanchaId")
        private String juegoRevanchaId;

        @JsonProperty("sorteoRevanchaId")
        private String sorteoRevanchaId;

        @JsonProperty("fechaCaducidadSorteo")
        private String fechaCaducidadSorteo;

        /** Etiqueta del numero principal, ej. "Numero de Carton (Pozo Millonario)". */
        @JsonProperty("nombreNumero")
        private String nombreNumero;
    }
}
