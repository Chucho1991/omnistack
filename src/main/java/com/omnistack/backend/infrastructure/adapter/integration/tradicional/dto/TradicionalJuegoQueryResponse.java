package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalJuegoQueryResponse {
    private String juegoId;
    private String nombre;
    private String descripcion;
}
