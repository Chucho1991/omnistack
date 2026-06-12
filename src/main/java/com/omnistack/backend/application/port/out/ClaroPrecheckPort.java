package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ClaroPrecheckCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

public interface ClaroPrecheckPort {
    ExternalTransactionResponse validateRecharge(ClaroPrecheckCommand command, String operationPath);
}
