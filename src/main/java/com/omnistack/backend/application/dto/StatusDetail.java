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
@Schema(description = "Estado funcional de la operacion")
public class StatusDetail {
    @Schema(example = "00")
    private String code;

    @Schema(example = "Transaccion exitosa")
    private String message;
}
