package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

/**
 * Response externo del login de Loteria.
 */
@Data
public class LoteriaLoginResponse {
    private String usuario;
    private String token;
    @JsonProperty("codError")
    private Integer errorCode;
    @JsonProperty("msgError")
    private String errorMessage;
    private Map<String, Object> raw = new LinkedHashMap<>();

    @com.fasterxml.jackson.annotation.JsonAnySetter
    void capture(String name, Object value) {
        raw.put(name, value);
    }
}
