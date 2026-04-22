package com.omnistack.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del endpoint de healthcheck de la aplicacion.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estado operativo basico del servicio")
public class HealthcheckResponse {

    @Schema(example = "UP")
    private String status;

    @Schema(example = "omnistack-backend")
    private String application;

    @Schema(example = "2026-04-22T12:00:00Z")
    private String timestamp;

    @Schema(example = "memory-v1")
    private String catalogVersion;
}
