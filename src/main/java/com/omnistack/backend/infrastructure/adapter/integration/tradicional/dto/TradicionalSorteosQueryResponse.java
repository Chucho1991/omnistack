package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private List<Sorteo> listaSorteos;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sorteo {
        private String sorteoId;
        private String nombre;
        private String fecha;
        private BigDecimal precio;
        private BigDecimal premioMayor;
        private Boolean disponible;
    }
}
