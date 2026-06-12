package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalVerifyCommand;

public interface TradicionalVerifyPort {
    ExternalTransactionResponse generateComprobante(TradicionalVerifyCommand command, String operationPath);
}
