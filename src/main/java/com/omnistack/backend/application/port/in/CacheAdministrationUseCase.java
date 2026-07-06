package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.CacheReloadResponse;

public interface CacheAdministrationUseCase {

    CacheReloadResponse reloadAll();
}
