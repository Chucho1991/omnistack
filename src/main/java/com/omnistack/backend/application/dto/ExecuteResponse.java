package com.omnistack.backend.application.dto;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
/**
 * DTO de salida para la ejecucion transaccional.
 */
public class ExecuteResponse extends BaseTransactionResponse {
}
