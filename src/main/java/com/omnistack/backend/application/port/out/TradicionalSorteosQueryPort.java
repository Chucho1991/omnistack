package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalSorteosQueryCommand;

public interface TradicionalSorteosQueryPort {
    ExternalTransactionResponse querySorteos(TradicionalSorteosQueryCommand command, String operationPath);
}
