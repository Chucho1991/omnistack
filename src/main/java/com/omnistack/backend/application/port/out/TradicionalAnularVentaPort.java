package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.TradicionalAnularVentaCommand;

public interface TradicionalAnularVentaPort {
    ExternalTransactionResponse anularVenta(TradicionalAnularVentaCommand command, String operationPath);
}
