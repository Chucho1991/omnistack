package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Response de GenerarComprobanteVenta. El proveedor real envuelve el PDF en
 * un objeto JSON (fileName/contentType/base64), no retorna el binario crudo.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalComprobanteResponse {
    private String fileName;
    private String contentType;
    private String base64;
}
