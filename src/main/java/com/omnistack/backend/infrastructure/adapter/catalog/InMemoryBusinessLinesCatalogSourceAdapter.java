package com.omnistack.backend.infrastructure.adapter.catalog;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.port.out.BusinessLinesCatalogSourcePort;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fallback en memoria para business-lines cuando la fuente Oracle esta deshabilitada.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.business-lines", name = "source", havingValue = "memory")
public class InMemoryBusinessLinesCatalogSourceAdapter implements BusinessLinesCatalogSourcePort {

    private final InMemoryCatalogSourceAdapter inMemoryCatalogSourceAdapter;

    @Override
    public CatalogSnapshot loadCatalogSnapshot(BusinessLinesRequest request) {
        return inMemoryCatalogSourceAdapter.loadCatalogSnapshot();
    }
}
