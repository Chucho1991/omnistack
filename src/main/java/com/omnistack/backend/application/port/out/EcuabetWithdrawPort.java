package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para la ejecucion de nota de retiro del proveedor ECUABET.
 */
public interface EcuabetWithdrawPort {

    /**
     * Ejecuta la nota de retiro en ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse withdraw(EcuabetWithdrawCommand command, String operationPath);
}
