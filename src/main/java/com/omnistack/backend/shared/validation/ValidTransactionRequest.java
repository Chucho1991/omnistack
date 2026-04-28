package com.omnistack.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida reglas especificas de solicitudes transaccionales.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TransactionRequestValidator.class)
public @interface ValidTransactionRequest {
    /**
     * Mensaje de error por defecto.
     *
     * @return mensaje de validacion
     */
    String message() default "La combinacion de campos de la solicitud no es valida";

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
