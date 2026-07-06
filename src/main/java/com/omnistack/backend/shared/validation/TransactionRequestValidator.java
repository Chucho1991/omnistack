package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * Validador de datos minimos requeridos para solicitudes transaccionales.
 * Ejecuta una cadena de {@link TransactionValidationRule} independientes —
 * agregar una regla nueva es agregar una clase a la lista RULES, sin tocar
 * las reglas existentes ni esta clase.
 */
public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, BaseTransactionRequest> {

    private static final List<TransactionValidationRule> RULES = List.of(
            new IdentifierRequiredRule()
    );

    /**
     * Ejecuta la cadena de reglas de validacion. Corta en la primera que falle.
     *
     * @param value solicitud base a validar.
     * @param context contexto de validacion de Bean Validation.
     * @return {@code true} si la solicitud cumple todas las reglas aplicables.
     */
    @Override
    public boolean isValid(BaseTransactionRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        for (TransactionValidationRule rule : RULES) {
            if (!rule.isSatisfiedBy(value)) {
                return false;
            }
        }
        return true;
    }
}
