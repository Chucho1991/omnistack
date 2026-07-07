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
public class TradicionalSorteosQueryResponse {
    private Object codError;
    private String msgError;
    @JsonProperty("listaDetalle")
    private List<Sorteo> listaSorteos;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sorteo {
        private String sorteoId;

        @JsonProperty("nombreSorteo")
        private String nombre;

        @JsonProperty("fechaSorteo")
        private String fecha;

        @JsonProperty("pvp")
        private BigDecimal precio;

        @JsonProperty("valorPremio")
        private BigDecimal premioMayor;

        @JsonProperty("cantidadFraccion")
        private Integer cantidadFraccion;

        @JsonProperty("tieneRevancha")
        private Boolean tieneRevancha;

        @JsonProperty("juegoRevanchaId")
        private String juegoRevanchaId;

        @JsonProperty("sorteoRevanchaId")
        private String sorteoRevanchaId;
    }
}
