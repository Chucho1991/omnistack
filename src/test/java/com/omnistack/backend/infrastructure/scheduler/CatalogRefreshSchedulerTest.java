package com.omnistack.backend.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.omnistack.backend.application.service.CatalogCacheService;
import com.omnistack.backend.infrastructure.adapter.catalog.InMemoryCatalogSourceAdapter;
import org.junit.jupiter.api.Test;

class CatalogRefreshSchedulerTest {

    @Test
    void shouldRefreshCatalog() {
        CatalogCacheService cacheService = new CatalogCacheService(new InMemoryCatalogSourceAdapter());
        CatalogRefreshScheduler scheduler = new CatalogRefreshScheduler(cacheService);

        scheduler.initialLoad();

        assertFalse(cacheService.getCurrentSnapshot().getCategories().isEmpty());
    }
}
