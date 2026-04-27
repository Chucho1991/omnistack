package com.omnistack.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseTransactionResponse extends BaseResponse {
    @Schema(example = "f0908f64-9145-45cf-a22c-c36bca604372")
    private String uuid;

    @Schema(example = "OK")
    private String providerCode;

    @Schema(example = "Operacion aprobada")
    private String providerMessage;
}
