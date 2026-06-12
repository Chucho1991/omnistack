package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
    private List<Object> listaSUE;

    public boolean isSuccess() {
        return "0".equals(String.valueOf(codError));
    }
}
