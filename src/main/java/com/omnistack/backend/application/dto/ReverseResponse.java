package com.omnistack.backend.application.dto;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
/**
 * DTO de salida para la operacion de reverso.
 */
public class ReverseResponse extends BaseTransactionResponse {
}
