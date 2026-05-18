package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonAlias("Code")
    private String code;
    private String transactionId;
    @JsonAlias({"message", "msg", "Message"})
    private String message;
    private final Map<String, Object> raw = new LinkedHashMap<>();

    /**
     * Normaliza el indicador de error externo cuando llega como entero,
     * booleano o texto.
     *
     * @param value valor recibido en el atributo {@code error}
     */
    @JsonProperty("error")
    public void setErrorValue(Object value) {
        this.error = normalizeError(value);
    }

    /**
     * Normaliza el indicador de error externo cuando llega como {@code Error}.
     *
     * @param value valor recibido en el atributo {@code Error}
     */
    @JsonProperty("Error")
    public void setUppercaseErrorValue(Object value) {
        this.error = normalizeError(value);
    }

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

    private Integer normalizeError(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? 1 : 0;
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue();
        }
        String textValue = String.valueOf(value).trim();
        if (textValue.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(textValue)) {
            return 1;
        }
        if ("false".equalsIgnoreCase(textValue)) {
            return 0;
        }
        return Integer.valueOf(textValue);
    }
}
