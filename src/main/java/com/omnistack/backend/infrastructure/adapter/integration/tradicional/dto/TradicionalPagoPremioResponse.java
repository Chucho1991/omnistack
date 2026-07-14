package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de PagoPremioTicketTradicional (Tradicionales - EXECUTE CASH_OUT).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalPagoPremioResponse {
    private Object codError;
    private String msgError;
    private String usuario;
    private String transaccion;
    private String token;
    private Resultado resultado;

    public boolean isSuccess() {
        return "0".equals(String.valueOf(codError));
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resultado {
        @JsonProperty("PPR")
        private Ppr ppr;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ppr {
        @JsonProperty("MPI")
        private Object mpi;
        @JsonProperty("PCI")
        private Object pci;
        @JsonProperty("REF")
        private Object ref;
    }
}
