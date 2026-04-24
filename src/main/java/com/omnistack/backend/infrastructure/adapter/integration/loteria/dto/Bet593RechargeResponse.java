package com.omnistack.backend.infrastructure.adapter.integration.loteria.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo para recarga de saldo BET593 en Loteria Nacional.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Bet593RechargeResponse {
    private String usuario;
    private String token;
    private String operacion;
    private Integer codError;
    private String msgError;
    private String resultado;
    private String cuentaweb;
    private String nombre;
    private String apellido;
    private String tipoDocumento;
    private String valor;
    private String fecharecarga;
    private String recargaid;
    private String serialnumber;
    private String estado;
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
