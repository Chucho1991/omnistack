package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.domain.enums.MovementType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TransactionRequestValidator implements ConstraintValidator<ValidTransactionRequest, BaseTransactionRequest> {

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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
