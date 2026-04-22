package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.ReverseRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ReverseRequestValidator implements ConstraintValidator<ValidReverseRequest, ReverseRequest> {

    @Override
    public boolean isValid(ReverseRequest value, ConstraintValidatorContext context) {
        return value == null || (value.getMotivo() != null && !value.getMotivo().isBlank());
    }
}
