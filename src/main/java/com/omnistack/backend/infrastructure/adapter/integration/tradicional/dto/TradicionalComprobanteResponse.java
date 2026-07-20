package com.omnistack.backend.infrastructure.adapter.integration.tradicional.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Response de GenerarComprobanteVenta. Con formato=0 (PDF) el proveedor envuelve el PDF en
 * un objeto JSON (fileName/contentType/base64 planos); con formato=1 (PNG) o formato=2 (JPG)
 * responde una lista (total/formato/imagenes[]) — se modelan ambas formas en la misma clase,
 * los campos de la forma no usada quedan simplemente null.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TradicionalComprobanteResponse {
    private String fileName;
    private String contentType;
    private String base64;

    private Integer total;
    private String formato;
    private List<Imagen> imagenes;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Imagen {
        private String fileName;
        private String contentType;
        private String base64;
    }
}
