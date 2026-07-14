package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3PayTicketCommand;

/**
 * Puerto de salida para pagar un ticket Pega3 (PagarTicket - EXECUTE).
 */
public interface Pega3PayTicketPort {

    /**
     * Confirma el pago de un ticket de apuesta en Pega3.
     *
     * @param command request interno normalizado con numero de ticket y monto
     * @param operationPath ruta configurada del endpoint externo
     * @param wsKey clave WS_KEY de la operacion invocante, usada para el log de auditoria
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse payTicket(Pega3PayTicketCommand command, String operationPath, String wsKey);
}
