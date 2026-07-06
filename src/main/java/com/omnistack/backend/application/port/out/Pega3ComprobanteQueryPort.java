package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3ComprobanteQueryCommand;

/**
 * Puerto de salida para generar el comprobante PDF de una venta Pega3 (GenerarComprobantePega - VERIFY).
 */
public interface Pega3ComprobanteQueryPort {

    /**
     * Genera el comprobante de venta en PDF (Base64) para una venta Pega3 ya confirmada.
     *
     * @param command request interno normalizado con ventaId/idUsuario/transaccion
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor, con el PDF en payload["comprobante_b64"]
     */
    ExternalTransactionResponse generarComprobante(Pega3ComprobanteQueryCommand command, String operationPath);
}
