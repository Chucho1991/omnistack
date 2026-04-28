package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo para nota de retiro BET593 en Loteria Nacional.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bet593WithdrawResponse {
    private Integer codError;
    private String msgError;
    private String usuario;
    private String operacion;
    private String token;
    private Long ordenPagoId;
    private String identificacion;
    private String valor;
    private String numeroTransaccion;
    private String nombre;
    private String fecha;
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
