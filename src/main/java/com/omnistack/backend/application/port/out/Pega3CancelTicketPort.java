package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3CancelTicketCommand;

/**
 * Puerto de salida para cancelar un ticket Pega3 (CancelarTicket - REVERSE).
 */
public interface Pega3CancelTicketPort {

    /**
     * Cancela un ticket de apuesta en el proveedor Pega3.
     *
     * @param command request interno normalizado con numero de ticket
     * @param operationPath ruta configurada del endpoint externo
     * @return respuesta normalizada del proveedor
     */
    ExternalTransactionResponse cancelTicket(Pega3CancelTicketCommand command, String operationPath);
}
