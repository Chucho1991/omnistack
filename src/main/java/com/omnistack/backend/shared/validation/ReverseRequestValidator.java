package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.ReverseRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador de reglas minimas para solicitudes de reverso.
 */
public class ReverseRequestValidator implements ConstraintValidator<ValidReverseRequest, ReverseRequest> {

    /**
     * Evalua que la solicitud incluya motivo cuando se envia contenido.
     *
     * @param value solicitud a validar.
     * @param context contexto de validacion de Bean Validation.
     * @return {@code true} si cumple la regla o es nula; caso contrario {@code false}.
     */
    @Override
    public boolean isValid(ReverseRequest value, ConstraintValidatorContext context) {
        return value == null || (value.getMotivo() != null && !value.getMotivo().isBlank());
    }
}
