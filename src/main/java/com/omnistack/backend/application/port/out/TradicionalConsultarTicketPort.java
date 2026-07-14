package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalConsultarTicketCommand;

/**
 * Puerto de salida para consultar si un boleto de Tradicionales tiene premio (ConsultarTicket - PRECHECK CASH_OUT).
 */
public interface TradicionalConsultarTicketPort {

    ExternalTransactionResponse consultarTicket(TradicionalConsultarTicketCommand command, String operationPath);
}
