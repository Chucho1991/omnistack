package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.CacheReloadResponse;
import com.omnistack.backend.application.port.in.CacheAdministrationUseCase;
import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.application.dto.StatusDetail;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheAdministrationService implements CacheAdministrationUseCase {

    private final ProviderConfigService providerConfigService;
    private final ProviderWsService providerWsService;
    private final ProviderWsDefsService providerWsDefsService;
    private final AdItemServicioService adItemServicioService;
    private final CatalogCacheService catalogCacheService;
    private final BusinessLinesCatalogCacheService businessLinesCatalogCacheService;
    private final Clock clock;

    @Override
    public CacheReloadResponse reloadAll() {
        log.info("Cache reload iniciado manualmente");

        providerConfigService.init();
        providerWsService.init();
        providerWsDefsService.init();
        adItemServicioService.init();
        catalogCacheService.refreshCatalog();
        businessLinesCatalogCacheService.clearCache();

        OffsetDateTime reloadedAt = OffsetDateTime.ofInstant(clock.instant(), clock.getZone());
        log.info("Cache reload completado. reloadedAt={}", reloadedAt);

        return CacheReloadResponse.builder()
                .errorFlag(false)
                .status(new StatusDetail(StatusCodes.SUCCESS, "Caches recargados correctamente"))
                .reloadedAt(reloadedAt)
                .caches(List.of(
                        "provider_config",
                        "provider_ws",
                        "provider_ws_defs",
                        "ad_item_servicio",
                        "catalog",
                        "business_lines"))
                .build();
    }
}
