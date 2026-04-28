package com.omnistack.backend.application.dto;

import com.omnistack.backend.shared.validation.ValidTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO de entrada para ejecutar una transaccion en proveedor externo.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidTransactionRequest
@Schema(description = "Solicitud de ejecucion transaccional")
public class ExecuteRequest extends BaseTransactionRequest {
    @DecimalMin(value = "0.0", inclusive = false)
    @Schema(example = "25.50")
    private BigDecimal amount;
}
