package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.ProviderCallLog;

public interface WsExtLogPort {
    void log(ProviderCallLog entry);
}
