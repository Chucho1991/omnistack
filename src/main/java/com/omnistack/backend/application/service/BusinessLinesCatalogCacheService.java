package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.port.out.BusinessLinesCatalogSourcePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Cache por request para el endpoint business-lines.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessLinesCatalogCacheService {

    private final BusinessLinesCatalogSourcePort businessLinesCatalogSourcePort;
    private final AppProperties appProperties;
    private final Clock clock;
    private final ConcurrentMap<RequestCacheKey, CachedCatalogSnapshot> snapshots = new ConcurrentHashMap<>();

    /**
     * Obtiene el catalogo vigente para el request recibido, reutilizando cache durante el TTL configurado.
     *
     * @param request request del endpoint
     * @return snapshot del catalogo aplicable
     */
    public CatalogSnapshot getCatalogSnapshot(BusinessLinesRequest request) {
        Instant now = clock.instant();
        Duration ttl = Duration.ofHours(appProperties.getBusinessLines().getCache().getTtlHours());
        RequestCacheKey cacheKey = RequestCacheKey.from(request);

        CachedCatalogSnapshot cachedCatalogSnapshot = snapshots.compute(cacheKey, (key, currentValue) -> {
            if (currentValue != null && !currentValue.isExpired(now)) {
                return currentValue;
            }

            CatalogSnapshot snapshot = businessLinesCatalogSourcePort.loadCatalogSnapshot(request);
            Instant expiresAt = now.plus(ttl);
            log.info(
                    "Business lines catalog loaded from source. chain={}, store={}, pos={}, channelPos={}, expiresAt={}",
                    request.getChain(),
                    request.getStore(),
                    request.getPos(),
                    request.getChannelPos(),
                    expiresAt);
            return new CachedCatalogSnapshot(snapshot, expiresAt);
        });

        return cachedCatalogSnapshot.snapshot();
    }

    record CachedCatalogSnapshot(CatalogSnapshot snapshot, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return !expiresAt.isAfter(now);
        }
    }

    record RequestCacheKey(String chain, String store, String storeName, String pos, String channelPos) {
        static RequestCacheKey from(BusinessLinesRequest request) {
            return new RequestCacheKey(
                    request.getChain(),
                    request.getStore(),
                    request.getStoreName(),
                    request.getPos(),
                    Objects.requireNonNull(request.getChannelPos()).name());
        }
    }
}
