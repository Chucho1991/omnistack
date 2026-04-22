package com.omnistack.backend.shared.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ReverseRequestValidator.class)
public @interface ValidReverseRequest {
    String message() default "La solicitud de reverso no es valida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
