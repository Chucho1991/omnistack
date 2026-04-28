package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Respuesta base compartida por los contratos de salida de la API.
 */
@Data
@SuperBuilder
@NoArgsConstructor
public abstract class BaseResponse {
    @JsonProperty("is_error")
    @Schema(example = "false")
    private boolean errorFlag;
    private ErrorDetail error;
    private StatusDetail status;
}
