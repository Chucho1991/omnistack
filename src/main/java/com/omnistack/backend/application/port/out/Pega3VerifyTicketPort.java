package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3VerifyTicketCommand;

/**
 * Puerto de salida para consultar el estado de un ticket Pega3 (ConsultarTicket - VERIFY).
 */
public interface Pega3VerifyTicketPort {

    /**
     * Consulta el estado actual de un ticket de apuesta en Pega3.
     *
     * @param command request interno normalizado con numero de ticket
     * @param operationPath ruta configurada del endpoint externo
     * @param wsKey clave WS_KEY de la operacion invocante, usada para el log de auditoria
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse verifyTicket(Pega3VerifyTicketCommand command, String operationPath, String wsKey);
}
