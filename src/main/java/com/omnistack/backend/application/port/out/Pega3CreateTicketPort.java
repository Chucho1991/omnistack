package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3CreateTicketCommand;

/**
 * Puerto de salida para crear un ticket de apuesta en Pega3 (CrearTicket).
 */
public interface Pega3CreateTicketPort {

    /**
     * Crea un ticket de apuesta en el proveedor Pega3.
     *
     * @param command request interno normalizado con datos del ticket
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse createTicket(Pega3CreateTicketCommand command, String operationPath);
}
