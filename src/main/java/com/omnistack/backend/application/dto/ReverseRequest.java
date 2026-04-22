package com.omnistack.backend.application.dto;

import com.omnistack.backend.shared.validation.ValidReverseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidReverseRequest
@Schema(description = "Solicitud de reverso")
public class ReverseRequest extends BaseTransactionRequest {
    @NotBlank
    @Schema(example = "Reverso por timeout del proveedor")
    private String motivo;
}
