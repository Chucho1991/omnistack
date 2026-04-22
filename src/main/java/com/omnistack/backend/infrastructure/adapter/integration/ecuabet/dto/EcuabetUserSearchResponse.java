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
 * Response externo generico para los endpoints de busqueda ECUABET.
 * Conserva en {@code raw} los atributos no modelados explicitamente.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuabetUserSearchResponse {
    private String code;
    private Integer error;
    private String name;
    @JsonAlias("userId")
    private String userid;
    @JsonProperty("message")
    @JsonAlias("msg")
    private String message;
    private final Map<String, Object> raw = new LinkedHashMap<>();

    /**
     * Registra atributos adicionales del response externo que no forman parte
     * del contrato tipado.
     *
     * @param key nombre del atributo recibido desde el proveedor externo
     * @param value valor asociado al atributo recibido
     */
    @JsonAnySetter
    public void addRawField(String key, Object value) {
        raw.put(key, value);
    }
}
