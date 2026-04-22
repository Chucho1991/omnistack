package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.CatalogCachePort;
import com.omnistack.backend.application.port.out.CatalogSourcePort;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.CatalogNotFoundException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de cache en memoria para catalogos.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogCacheService implements CatalogCachePort {

    private final CatalogSourcePort catalogSourcePort;
    private final AtomicReference<CatalogSnapshot> currentSnapshot = new AtomicReference<>(
            CatalogSnapshot.builder()
                    .categories(Collections.emptyList())
                    .services(Collections.emptyList())
                    .loadedAt(OffsetDateTime.now())
                    .version("bootstrap")
                    .build());

    public synchronized void refreshCatalog() {
        try {
            CatalogSnapshot snapshot = catalogSourcePort.loadCatalogSnapshot();
            currentSnapshot.set(snapshot);
            log.info("Catalog snapshot refreshed. version={}, loadedAt={}", snapshot.getVersion(), snapshot.getLoadedAt());
        } catch (Exception exception) {
            log.error("Catalog refresh failed. Keeping last valid snapshot.", exception);
        }
    }

    @Override
    public CatalogSnapshot getCurrentSnapshot() {
        return currentSnapshot.get();
    }

    @Override
    public Optional<ServiceDefinition> findService(
            String categoryCode,
            String subcategoryCode,
            String providerCode,
            String rmsItemCode) {
        return currentSnapshot.get().getServices().stream()
                .filter(service -> service.getCategoryCode().equalsIgnoreCase(categoryCode))
                .filter(service -> service.getSubcategoryCode().equalsIgnoreCase(subcategoryCode))
                .filter(service -> service.getServiceProviderCode().equalsIgnoreCase(providerCode))
                .filter(service -> service.getRmsItemCode().equalsIgnoreCase(rmsItemCode))
                .findFirst();
    }

    @Override
    public List<ServiceDefinition> findServicesByMovementType(String movementType) {
        return currentSnapshot.get().getServices().stream()
                .filter(service -> movementType == null || service.getMovementType().name().equalsIgnoreCase(movementType))
                .collect(Collectors.toList());
    }

    public ServiceDefinition getRequiredService(
            String categoryCode,
            String subcategoryCode,
            String providerCode,
            String rmsItemCode) {
        return findService(categoryCode, subcategoryCode, providerCode, rmsItemCode)
                .orElseThrow(() -> new CatalogNotFoundException("No se encontro configuracion del servicio solicitada"));
    }
}
