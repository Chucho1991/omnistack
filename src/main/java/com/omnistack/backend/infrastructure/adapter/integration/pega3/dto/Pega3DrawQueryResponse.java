package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de sorteo activo Pega3 (ObtieneSorteosActivoxJuego).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3DrawQueryResponse {
    private Integer drawNumber;
    private String drawDate;
    private String message;
}
