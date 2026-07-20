package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de ConsultarTicket (Tradicionales - PRECHECK CASH_OUT).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalConsultarTicketResponse {
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
        @JsonProperty("REF")
        private Object ref;
        @JsonProperty("BPR")
        private List<Bpr> bpr;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Bpr {
        @JsonProperty("COB")
        private String cob;
        @JsonProperty("CLA")
        private String cla;
        @JsonProperty("JID")
        private String jid;
        @JsonProperty("JNO")
        private String jno;
        @JsonProperty("SID")
        private String sid;
        /** El ejemplo JSON del spec usa "PRE"/"MON", pero la tabla de campos del mismo
         * documento (v0005) los llama "PRE1"/"MON1" — se aceptan ambos por seguridad. */
        @JsonProperty("PRE")
        @JsonAlias("PRE1")
        private String pre;
        @JsonProperty("MON")
        @JsonAlias("MON1")
        private String mon;
        @JsonProperty("VDE")
        private String vde;
        @JsonProperty("VAL")
        private String val;
        @JsonProperty("MEN")
        private String men;
        @JsonProperty("COD")
        private String cod;
    }
}
