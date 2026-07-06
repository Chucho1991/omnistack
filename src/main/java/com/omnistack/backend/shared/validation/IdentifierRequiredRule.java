package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.domain.enums.MovementType;

/**
 * Exige un identificador de cliente (phone/document/userid para CASH_IN,
 * withdrawId/password para CASH_OUT) en operaciones que comprometen al
 * cliente (EXECUTE, REVERSE, VERIFY).
 * <p>
 * No aplica a PrecheckRequest ni CreateTicketRequest: son operaciones de
 * consulta/reserva de catalogo, y varian demasiado por proveedor como para
 * exigir un identificador de forma universal (ej. Tradicionales no identifica
 * al comprador hasta el EXECUTE; ECUABET/BET593 si lo necesitan, pero eso lo
 * validan sus propias strategies de PRECHECK).
 */
public class IdentifierRequiredRule implements TransactionValidationRule {

    @Override
    public boolean isSatisfiedBy(BaseTransactionRequest request) {
        if (request instanceof PrecheckRequest || request instanceof CreateTicketRequest) {
            return true;
        }

        if (request.getMovementType() == null) {
            return hasText(request.getPhone())
                    || hasText(request.getDocument())
                    || hasText(request.getUserid())
                    || hasText(request.getWithdrawId())
                    || hasText(request.getPassword());
        }

        if (request.getMovementType() == MovementType.CASH_IN) {
            return hasText(request.getPhone()) || hasText(request.getDocument()) || hasText(request.getUserid());
        }

        if (request.getMovementType() == MovementType.CASH_OUT) {
            return hasText(request.getWithdrawId()) || hasText(request.getPassword());
        }

        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
