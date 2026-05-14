package com.omnistack.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Respuesta uniforme para errores controlados por la API.
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Respuesta uniforme de error")
public class ErrorResponse extends BaseResponse {
}
