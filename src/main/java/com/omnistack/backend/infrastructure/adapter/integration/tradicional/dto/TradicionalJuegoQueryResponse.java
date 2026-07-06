package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de RecuperarJuegosPorMedio. El proveedor real envuelve la
 * lista de juegos en un objeto (usuario/token/transaccion/codError/msgError),
 * no en un array plano.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalJuegoQueryResponse {
    private String usuario;
    private String token;
    private String transaccion;
    private Object codError;
    private String msgError;
    private List<Juego> listaDetalle;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Juego {
        private String juegoId;
        private String nombreJuego;
        private Boolean estadoJuego;
        private Boolean juegoVisible;
    }
}
