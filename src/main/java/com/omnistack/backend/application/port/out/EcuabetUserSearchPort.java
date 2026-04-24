package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

/**
 * Puerto de salida para la operacion Buscar usuario del proveedor ECUABET.
 */
public interface EcuabetUserSearchPort {

    /**
     * Ejecuta la busqueda de usuario en ECUABET.
     *
     * @param command request interno normalizado para la operacion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse searchUser(EcuabetUserSearchCommand command, String operationPath);
}
