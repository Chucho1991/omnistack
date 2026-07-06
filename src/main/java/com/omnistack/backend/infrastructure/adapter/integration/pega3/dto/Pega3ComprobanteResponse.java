package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de GenerarComprobantePega (VERIFY) — PDF de la venta en Base64.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3ComprobanteResponse {
    private String fileName;
    private String contentType;
    private String base64;
}
