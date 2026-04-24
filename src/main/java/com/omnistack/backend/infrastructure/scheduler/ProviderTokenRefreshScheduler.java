package com.omnistack.backend.infrastructure.scheduler;

import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Inicializador de tokens dinamicos de proveedores al arranque del aplicativo.
 */
@Component
@RequiredArgsConstructor
public class ProviderTokenRefreshScheduler {

    private final ProviderTokenAdministrationUseCase providerTokenAdministrationUseCase;

    /**
     * Ejecuta el refresco inicial de tokens dinamicos configurados.
     */
    @PostConstruct
    public void refreshOnStartup() {
        providerTokenAdministrationUseCase.refreshTokensOnStartup();
    }
}
