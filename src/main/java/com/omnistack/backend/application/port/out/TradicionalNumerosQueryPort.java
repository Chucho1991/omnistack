package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalNumerosQueryCommand;

public interface TradicionalNumerosQueryPort {
    ExternalTransactionResponse queryNumeros(TradicionalNumerosQueryCommand command, String operationPath);
}
