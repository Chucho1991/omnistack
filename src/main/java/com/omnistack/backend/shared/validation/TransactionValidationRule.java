package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;

/**
 * Regla individual de validacion cruzada para solicitudes transaccionales.
 * Cada regla nueva se agrega como una clase propia e independiente, sin
 * modificar las reglas existentes ni el validador que las ejecuta
 * (ver {@link TransactionRequestValidator}).
 */
public interface TransactionValidationRule {

    /**
     * Evalua la regla contra el request. Si la regla no aplica al tipo de
     * request recibido (ej. una regla de EXECUTE evaluada contra un
     * PrecheckRequest), debe devolver {@code true} — "no aplica" nunca debe
     * bloquear la cadena.
     *
     * @param request solicitud a evaluar
     * @return {@code true} si la regla no aplica o si se cumple; {@code false} si aplica y no se cumple
     */
    boolean isSatisfiedBy(BaseTransactionRequest request);
}
