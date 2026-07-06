package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.shared.validation.ValidTransactionRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * DTO de entrada para verificar el estado de una transaccion.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ValidTransactionRequest
@Schema(description = "Solicitud de verificacion posterior")
public class VerifyRequest extends BaseTransactionRequest {
    @JsonProperty("transaccion")
    @Schema(example = "12345678", description = "Numero de transaccion interna del proveedor, requerido por GenerarComprobantePega (solo Pega3). Si no se envia, no se genera el comprobante PDF.")
    private String transaccion;
}
