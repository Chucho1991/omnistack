package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo del endpoint de ejecucion de nota de retiro ECUABET.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuabetWithdrawResponse {
    private Integer error;
    private String code;
    private String transactionId;
    @JsonAlias({"message", "msg"})
    private String message;
    private final Map<String, Object> raw = new LinkedHashMap<>();

    /**
     * Registra atributos adicionales del response externo que no forman parte del contrato tipado.
     *
     * @param key nombre del atributo recibido desde el proveedor externo
     * @param value valor asociado al atributo recibido
     */
    @JsonAnySetter
    public void addRawField(String key, Object value) {
        raw.put(key, value);
    }
}
