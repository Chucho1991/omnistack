package com.omnistack.backend.application.dto;

import com.omnistack.backend.shared.validation.ValidReverseRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
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
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "25.50")
    private BigDecimal amount;

    @NotBlank
    @Schema(example = "Reverso por timeout del proveedor")
    private String motivo;
}
