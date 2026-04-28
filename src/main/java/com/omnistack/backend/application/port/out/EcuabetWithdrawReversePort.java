package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para reversar notas de retiro del proveedor ECUABET.
 */
public interface EcuabetWithdrawReversePort {

    /**
     * Ejecuta el reverso de nota de retiro en ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse reverseWithdraw(EcuabetWithdrawCommand command, String operationPath);
}
