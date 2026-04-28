package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para reversar notas de retiro BET593 mediante Loteria Nacional.
 */
public interface Bet593WithdrawReversePort {

    /**
     * Reversa una nota de retiro BET593 en el endpoint externo configurado.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse reverseWithdraw(Bet593WithdrawCommand command, String operationPath);
}
