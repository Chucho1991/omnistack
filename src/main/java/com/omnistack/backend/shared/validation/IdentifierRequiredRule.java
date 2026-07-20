package com.omnistack.backend.shared.validation;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.domain.enums.MovementType;

/**
 * Exige un identificador (phone/document/userid para CASH_IN, withdrawId/password
 * para CASH_OUT, o authorization en cualquier caso) en operaciones que comprometen
 * al cliente (EXECUTE, REVERSE, VERIFY). authorization identifica una transaccion
 * ya ejecutada (ej. VERIFY de Tradicionales solo necesita el ventaId via authorization,
 * no vuelve a pedir document/phone/userid del comprador).
 * <p>
 * No aplica a PrecheckRequest ni CreateTicketRequest: son operaciones de
 * consulta/reserva de catalogo, y varian demasiado por proveedor como para
 * exigir un identificador de forma universal (ej. Tradicionales no identifica
 * al comprador hasta el EXECUTE; ECUABET/BET593 si lo necesitan, pero eso lo
 * validan sus propias strategies de PRECHECK).
 * <p>
 * Tampoco aplica a un ExecuteRequest con "ticket_data" (Pega3): desde que
 * CrearTicket se llama directo en EXECUTE (ya no hay CREATE_TICKET separado
 * para este proveedor, ver LoteriaPega3ExecuteStrategy), este EXECUTE cumple
 * el mismo rol que antes tenia CREATE_TICKET — vende el ticket sin identificar
 * previamente al comprador, no corresponde exigirle phone/document/authorization.
 */
public class IdentifierRequiredRule implements TransactionValidationRule {

    @Override
    public boolean isSatisfiedBy(BaseTransactionRequest request) {
        if (request instanceof PrecheckRequest || request instanceof CreateTicketRequest) {
            return true;
        }
        if (request instanceof ExecuteRequest executeRequest && executeRequest.getTicketData() != null) {
            return true;
        }

        if (request.getMovementType() == null) {
            return hasText(request.getPhone())
                    || hasText(request.getDocument())
                    || hasText(request.getUserid())
                    || hasText(request.getWithdrawId())
                    || hasText(request.getPassword())
                    || hasText(request.getAuthorization());
        }

        if (request.getMovementType() == MovementType.CASH_IN) {
            return hasText(request.getPhone()) || hasText(request.getDocument()) || hasText(request.getUserid())
                    || hasText(request.getAuthorization());
        }

        if (request.getMovementType() == MovementType.CASH_OUT) {
            return hasText(request.getWithdrawId()) || hasText(request.getPassword())
                    || hasText(request.getAuthorization());
        }

        return true;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
