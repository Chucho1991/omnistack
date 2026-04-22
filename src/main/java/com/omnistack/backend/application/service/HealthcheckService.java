package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.HealthcheckResponse;
import com.omnistack.backend.application.port.in.HealthcheckUseCase;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Caso de uso para consulta de salud basica de la aplicacion.
 */
@Service
@RequiredArgsConstructor
public class HealthcheckService implements HealthcheckUseCase {

    private final CatalogCacheService catalogCacheService;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Obtiene el estado operativo actual de la API.
     *
     * @return respuesta de healthcheck
     */
    @Override
    public HealthcheckResponse getHealthcheck() {
        return HealthcheckResponse.builder()
                .status("UP")
                .application(applicationName)
                .timestamp(OffsetDateTime.now().toString())
                .catalogVersion(catalogCacheService.getCurrentSnapshot().getVersion())
                .build();
    }
}
