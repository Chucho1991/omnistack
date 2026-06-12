package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalJuegoQueryCommand;

public interface TradicionalJuegoQueryPort {
    ExternalTransactionResponse queryJuegos(TradicionalJuegoQueryCommand command, String operationPath);
}
