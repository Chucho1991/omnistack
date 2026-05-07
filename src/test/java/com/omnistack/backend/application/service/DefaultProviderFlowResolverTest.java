package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.CatalogSnapshot;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.infrastructure.adapter.integration.DefaultProviderTransactionStrategy;
import com.omnistack.backend.infrastructure.adapter.integration.LoteriaBet593PrecheckStrategy;
import com.omnistack.backend.infrastructure.adapter.integration.MockExternalProviderClient;
import com.omnistack.backend.shared.exception.CatalogNotFoundException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

    @Test
    void shouldResolveBet593PrecheckToRealStrategyWhenMockStrategyIsPresent() {
        ServiceDefinition bet593Service = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("100708850")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
        CatalogCacheService cacheService = new CatalogCacheService(() -> CatalogSnapshot.builder()
                .services(List.of(bet593Service))
                .loadedAt(OffsetDateTime.now())
                .version("test")
                .build());
        cacheService.refreshCatalog();

        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("100708850");
        capabilityProperties.getCashin().setPath("/APIVentasLoteria/api/Ventas/RecargarBet593");
        provider.getServices().put("PRECHECK", capabilityProperties);
        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("loteria", provider);

        LoteriaBet593PrecheckStrategy realStrategy = new LoteriaBet593PrecheckStrategy(
                (command, operationPath) -> null,
                appProperties);
        DefaultProviderFlowResolver resolver = new DefaultProviderFlowResolver(
                cacheService,
                List.of(realStrategy, new DefaultProviderTransactionStrategy(new MockExternalProviderClient())));

        var request = PrecheckRequest.builder()
                .uuid("uuid-bet593")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("100708850")
                .amount(BigDecimal.TEN)
                .document("0901111112")
                .build();

        var selection = resolver.resolve(request, Capability.PRECHECK);

        assertEquals(LoteriaBet593PrecheckStrategy.class, selection.getStrategy().getClass());
    }
}
