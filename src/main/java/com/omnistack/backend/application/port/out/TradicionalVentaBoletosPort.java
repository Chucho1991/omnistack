package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalVentaBoletosCommand;

public interface TradicionalVentaBoletosPort {
    ExternalTransactionResponse ventaBoletos(TradicionalVentaBoletosCommand command, String operationPath);
}
