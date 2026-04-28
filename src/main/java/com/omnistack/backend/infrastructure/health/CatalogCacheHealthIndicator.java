package com.omnistack.backend.infrastructure.health;

import com.omnistack.backend.application.service.CatalogCacheService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Healthcheck basico del cache de catalogos.
 */
@Component
public class CatalogCacheHealthIndicator implements HealthIndicator {

    private final CatalogCacheService catalogCacheService;

    /**
     * Crea el indicador de salud del cache de catalogos.
     *
     * @param catalogCacheService servicio de cache de catalogos
     */
    public CatalogCacheHealthIndicator(CatalogCacheService catalogCacheService) {
        this.catalogCacheService = catalogCacheService;
    }

    @Override
    public Health health() {
        var snapshot = catalogCacheService.getCurrentSnapshot();
        return Health.up()
                .withDetail("catalogVersion", snapshot.getVersion())
                .withDetail("loadedAt", snapshot.getLoadedAt())
                .build();
    }
}
