package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalFigurasQueryCommand;

public interface TradicionalFigurasQueryPort {
    ExternalTransactionResponse queryFiguras(TradicionalFigurasQueryCommand command, String operationPath);
}
