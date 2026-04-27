package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.EcuabetDepositCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para la operacion de deposito del proveedor ECUABET.
 */
public interface EcuabetDepositPort {

    /**
     * Ejecuta el deposito de saldo en ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse deposit(EcuabetDepositCommand command, String operationPath);
}
