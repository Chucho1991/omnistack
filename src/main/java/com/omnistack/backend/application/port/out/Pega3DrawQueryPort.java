package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3DrawQueryCommand;

/**
 * Puerto de salida para consultar el sorteo activo de Pega3 (ObtieneSorteosActivo).
 */
public interface Pega3DrawQueryPort {

    /**
     * Consulta el sorteo activo disponible para apostar.
     *
     * @param command request interno normalizado
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse queryActiveDraw(Pega3DrawQueryCommand command, String operationPath);
}
