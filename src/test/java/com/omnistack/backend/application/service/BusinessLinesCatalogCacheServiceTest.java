package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.port.out.BusinessLinesCatalogSourcePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class BusinessLinesCatalogCacheServiceTest {

    @Test
    void shouldReuseSnapshotWithinConfiguredTtl() {
        BusinessLinesCatalogSourcePort sourcePort = Mockito.mock(BusinessLinesCatalogSourcePort.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getBusinessLines().getCache().setTtlHours(6);
        Clock clock = Clock.fixed(Instant.parse("2026-04-22T12:00:00Z"), ZoneId.of("America/Guayaquil"));
        BusinessLinesCatalogCacheService service = new BusinessLinesCatalogCacheService(sourcePort, appProperties, clock);

        BusinessLinesRequest request = BusinessLinesRequest.builder()
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .build();

        CatalogSnapshot snapshot = CatalogSnapshot.builder()
                .categories(List.of())
                .services(List.of())
                .loadedAt(OffsetDateTime.now(clock))
                .version("v1")
                .build();

        when(sourcePort.loadCatalogSnapshot(request)).thenReturn(snapshot);

        CatalogSnapshot firstResponse = service.getCatalogSnapshot(request);
        CatalogSnapshot secondResponse = service.getCatalogSnapshot(request);

        assertSame(snapshot, firstResponse);
        assertSame(snapshot, secondResponse);
        verify(sourcePort, times(1)).loadCatalogSnapshot(request);
    }

    @Test
    void shouldReuseSnapshotWhenPointOfSaleFieldsChangeBeforeDatabaseIntegration() {
        BusinessLinesCatalogSourcePort sourcePort = Mockito.mock(BusinessLinesCatalogSourcePort.class);
        AppProperties appProperties = new AppProperties();
        appProperties.getBusinessLines().getCache().setTtlHours(6);
        Clock clock = Clock.fixed(Instant.parse("2026-04-22T12:00:00Z"), ZoneId.of("America/Guayaquil"));
        BusinessLinesCatalogCacheService service = new BusinessLinesCatalogCacheService(sourcePort, appProperties, clock);

        BusinessLinesRequest firstRequest = BusinessLinesRequest.builder()
                .chain("{{chain}}")
                .store("4")
                .storeName("{{storeName}}")
                .pos("{{pos}}")
                .channelPos(ChannelPos.POS)
                .build();
        BusinessLinesRequest secondRequest = BusinessLinesRequest.builder()
                .chain("9")
                .store("999")
                .storeName("OTRA TIENDA")
                .pos("POS-99")
                .channelPos(ChannelPos.POS)
                .build();

        CatalogSnapshot snapshot = CatalogSnapshot.builder()
                .categories(List.of())
                .services(List.of())
                .loadedAt(OffsetDateTime.now(clock))
                .version("v1")
                .build();

        when(sourcePort.loadCatalogSnapshot(firstRequest)).thenReturn(snapshot);

        CatalogSnapshot firstResponse = service.getCatalogSnapshot(firstRequest);
        CatalogSnapshot secondResponse = service.getCatalogSnapshot(secondRequest);

        assertSame(snapshot, firstResponse);
        assertSame(snapshot, secondResponse);
        verify(sourcePort, times(1)).loadCatalogSnapshot(firstRequest);
    }
}
