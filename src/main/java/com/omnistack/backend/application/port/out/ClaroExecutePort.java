package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ClaroExecuteCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;

public interface ClaroExecutePort {
    ExternalTransactionResponse processRecharge(ClaroExecuteCommand command, String operationPath);
}
