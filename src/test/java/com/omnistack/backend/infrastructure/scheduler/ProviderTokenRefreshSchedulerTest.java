package com.omnistack.backend.infrastructure.scheduler;

import static org.mockito.Mockito.verify;

import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProviderTokenRefreshSchedulerTest {

    @Test
    void shouldRefreshTokensOnStartup() {
        ProviderTokenAdministrationUseCase useCase = Mockito.mock(ProviderTokenAdministrationUseCase.class);
        ProviderTokenRefreshScheduler scheduler = new ProviderTokenRefreshScheduler(useCase);

        scheduler.refreshOnStartup();

        verify(useCase).refreshTokensOnStartup();
    }
}
