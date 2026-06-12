package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.RegistroTrx;

public interface RegistroTrxPort {
    void save(RegistroTrx entry);
}
