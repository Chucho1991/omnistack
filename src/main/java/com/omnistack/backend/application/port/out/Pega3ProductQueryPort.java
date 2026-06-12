package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3ProductQueryCommand;

/**
 * Puerto de salida para consultar el producto disponible en Pega3 (VentaProductos).
 */
public interface Pega3ProductQueryPort {

    /**
     * Consulta informacion del producto de juego Pega3.
     *
     * @param command request interno normalizado
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse queryProduct(Pega3ProductQueryCommand command, String operationPath);
}
