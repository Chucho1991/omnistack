package com.omnistack.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Detalle uniforme del error")
public class ErrorDetail {
    @Schema(example = "BUS-001")
    private String code;

    @Schema(example = "No existe configuracion para el servicio solicitado")
    private String message;
}
