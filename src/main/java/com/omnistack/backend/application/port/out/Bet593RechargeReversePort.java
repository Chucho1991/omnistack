package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.Bet593RechargeCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para reversar recargas BET593 mediante Loteria Nacional.
 */
public interface Bet593RechargeReversePort {

    /**
     * Reversa una recarga BET593 en el endpoint externo configurado.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse reverseRecharge(Bet593RechargeCommand command, String operationPath);
}
