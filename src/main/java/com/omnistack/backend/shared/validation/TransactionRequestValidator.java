package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.domain.enums.MovementType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador de datos minimos requeridos para solicitudes transaccionales.
 */
public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, BaseTransactionRequest> {

    /**
     * Verifica combinaciones validas de identificadores por tipo de movimiento.
     *
     * @param value solicitud base a validar.
     * @param context contexto de validacion de Bean Validation.
     * @return {@code true} si la solicitud es consistente con la regla definida.
     */
    @Override
    public boolean isValid(BaseTransactionRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        if (value.getMovementType() == null) {
            return hasText(value.getPhone())
                    || hasText(value.getDocument())
                    || hasText(value.getUserid())
                    || hasText(value.getWithdrawId())
                    || hasText(value.getPassword());
        }

        if (value.getMovementType() == MovementType.CASH_IN) {
            return hasText(value.getPhone()) || hasText(value.getDocument()) || hasText(value.getUserid());
        }

        if (value.getMovementType() == MovementType.CASH_OUT) {
            return hasText(value.getWithdrawId()) || hasText(value.getPassword());
        }

        return true;
    }

    /**
     * Determina si una cadena contiene texto no vacio.
     *
     * @param value valor a evaluar.
     * @return {@code true} cuando existe contenido no blanco.
     */
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
