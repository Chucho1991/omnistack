package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.EcuabetDepositCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para reversar depositos del proveedor ECUABET.
 */
public interface EcuabetDepositReversePort {

    /**
     * Ejecuta el reverso de deposito en ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse reverseDeposit(EcuabetDepositCommand command, String operationPath);
}
