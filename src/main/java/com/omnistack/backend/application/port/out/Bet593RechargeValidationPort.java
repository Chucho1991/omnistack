package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.Bet593RechargeCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para validar recargas BET593 mediante Loteria Nacional.
 */
public interface Bet593RechargeValidationPort {

    /**
     * Valida el estado de una recarga BET593 en el endpoint externo configurado.
     *
     * @param command request interno normalizado para la validacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse validateRecharge(Bet593RechargeCommand command, String operationPath);
}
