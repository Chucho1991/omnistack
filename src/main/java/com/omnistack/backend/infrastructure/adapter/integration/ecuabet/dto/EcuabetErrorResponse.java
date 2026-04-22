package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Error externo generico devuelto por ECUABET.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuabetErrorResponse {
    private String code;
    private String message;
    private String error;
    private final Map<String, Object> raw = new LinkedHashMap<>();

    @JsonAnySetter
    public void addRawField(String key, Object value) {
        raw.put(key, value);
    }
}
