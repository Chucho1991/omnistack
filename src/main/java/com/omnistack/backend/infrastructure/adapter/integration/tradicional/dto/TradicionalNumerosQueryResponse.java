package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
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
    private List<Numero> listaNumeros;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Numero {
        private String numero;
        private Boolean disponible;
        private BigDecimal precio;
    }
}
