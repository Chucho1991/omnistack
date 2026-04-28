package com.omnistack.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida reglas especificas de solicitudes de reverso.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReverseRequestValidator.class)
public @interface ValidReverseRequest {
    /**
     * Mensaje de error por defecto.
     *
     * @return mensaje de validacion
     */
    String message() default "La solicitud de reverso no es valida";

    /**
     * Grupos de validacion Bean Validation.
     *
     * @return grupos asociados
     */
    Class<?>[] groups() default {};

    /**
     * Payload Bean Validation asociado.
     *
     * @return payload configurado
     */
    Class<? extends Payload>[] payload() default {};
}
