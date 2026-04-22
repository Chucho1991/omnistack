package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

/**
 * Campo parametrico requerido por un servicio comercial.
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Campo requerido por el servicio para una capacidad concreta")
public class BusinessLineInputFieldResponse {
    String id;
    String label;
    String type;
    String capability;
    boolean required;
    String group;
    String conditional;
}
