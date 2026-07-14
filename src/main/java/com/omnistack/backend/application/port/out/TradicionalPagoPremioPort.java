package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalPagoPremioCommand;

/**
 * Puerto de salida para pagar el premio de un boleto de Tradicionales (PagoPremioTicketTradicional - EXECUTE CASH_OUT).
 */
public interface TradicionalPagoPremioPort {

    ExternalTransactionResponse pagoPremio(TradicionalPagoPremioCommand command, String operationPath);
}
