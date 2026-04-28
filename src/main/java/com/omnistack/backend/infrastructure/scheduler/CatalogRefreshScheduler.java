package com.omnistack.backend.infrastructure.scheduler;

import com.omnistack.backend.application.service.CatalogCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler de recarga del catalogo en memoria.
 */
@Component
@RequiredArgsConstructor
public class CatalogRefreshScheduler {

    private final CatalogCacheService catalogCacheService;

    /**
     * Ejecuta la carga inicial del catalogo al iniciar el componente.
     */
    @PostConstruct
    public void initialLoad() {
        catalogCacheService.refreshCatalog();
    }

    /**
     * Ejecuta la recarga periodica del catalogo.
     */
    @Scheduled(
            fixedDelayString = "${app.catalog.refresh.fixed-delay-ms}",
            initialDelayString = "${app.catalog.refresh.initial-delay-ms}")
    public void refreshCatalog() {
        catalogCacheService.refreshCatalog();
    }
}
