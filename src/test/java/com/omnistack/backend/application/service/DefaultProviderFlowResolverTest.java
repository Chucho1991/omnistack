package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.infrastructure.adapter.integration.DefaultProviderTransactionStrategy;
import com.omnistack.backend.infrastructure.adapter.integration.MockExternalProviderClient;
import com.omnistack.backend.shared.exception.CatalogNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultProviderFlowResolverTest {

    @Test
    void shouldResolveConfiguredStrategy() {
        CatalogCacheService cacheService = new CatalogCacheService(new com.omnistack.backend.infrastructure.adapter.catalog.InMemoryCatalogSourceAdapter());
        cacheService.refreshCatalog();
        DefaultProviderFlowResolver resolver = new DefaultProviderFlowResolver(
                cacheService,
                List.of(new DefaultProviderTransactionStrategy(new MockExternalProviderClient())));

        var request = PrecheckRequest.builder()
                .uuid("uuid-1")
                .chain("001")
                .store("0001")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .movementType(com.omnistack.backend.domain.enums.MovementType.CASH_IN)
                .categoryCode("REC")
                .subcategoryCode("CEL")
                .serviceProviderCode("CLARO")
                .rmsItemCode("900001")
                .amount(BigDecimal.TEN)
                .phone("0999999999")
                .build();

        var selection = resolver.resolve(request, Capability.PRECHECK);
        assertEquals("CLARO", selection.getServiceDefinition().getServiceProviderCode());
    }

    @Test
    void shouldFailWhenServiceDoesNotExist() {
        CatalogCacheService cacheService = new CatalogCacheService(new com.omnistack.backend.infrastructure.adapter.catalog.InMemoryCatalogSourceAdapter());
        cacheService.refreshCatalog();
        DefaultProviderFlowResolver resolver = new DefaultProviderFlowResolver(
                cacheService,
                List.of(new DefaultProviderTransactionStrategy(new MockExternalProviderClient())));

        var request = PrecheckRequest.builder()
                .uuid("uuid-1")
                .chain("001")
                .store("0001")
                .pos("POS-01")
                .channelPos(ChannelPos.POS)
                .movementType(com.omnistack.backend.domain.enums.MovementType.CASH_IN)
                .categoryCode("XXX")
                .subcategoryCode("YYY")
                .serviceProviderCode("ZZZ")
                .rmsItemCode("999")
                .amount(BigDecimal.TEN)
                .phone("0999999999")
                .build();

        assertThrows(CatalogNotFoundException.class, () -> resolver.resolve(request, Capability.PRECHECK));
    }
}
