package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.port.out.ProviderTokenLoginPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;
import com.omnistack.backend.shared.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProviderTokenServiceTest {

    @Test
    void shouldReturnConfiguredStaticToken() {
        ProviderTokenService service = new ProviderTokenService(
                appPropertiesWithStaticProvider(),
                command -> ProviderTokenLoginResult.builder().token("unused").build(),
                fixedClock());

        String token = service.getToken("1", "1", "1");

        assertEquals("ecuabet-static-token", token);
    }

    @Test
    void shouldRefreshDynamicTokenOnceWhileTokenIsStillValid() {
        AtomicInteger loginCalls = new AtomicInteger();
        ProviderTokenLoginPort loginPort = command -> ProviderTokenLoginResult.builder()
                .token("dynamic-token-" + loginCalls.incrementAndGet())
                .build();
        ProviderTokenService service = new ProviderTokenService(
                appPropertiesWithDynamicProvider(),
                loginPort,
                fixedClock());

        String firstToken = service.getToken("1", "1", "2");
        String secondToken = service.getToken("1", "1", "2");

        assertEquals("dynamic-token-1", firstToken);
        assertEquals("dynamic-token-1", secondToken);
        assertEquals(1, loginCalls.get());
    }

    @Test
    void shouldPreferMostSpecificConfigurationForContext() {
        AtomicInteger loginCalls = new AtomicInteger();
        ProviderTokenLoginPort loginPort = command -> ProviderTokenLoginResult.builder()
                .token(command.getCategoryCode() + "-" + command.getSubcategoryCode() + "-" + loginCalls.incrementAndGet())
                .build();
        ProviderTokenService service = new ProviderTokenService(
                appPropertiesWithGenericAndSpecificDynamicProviders(),
                loginPort,
                fixedClock());

        String token = service.getToken("1", "2", "2");

        assertEquals("1-2-1", token);
    }

    @Test
    void shouldRejectManualRefreshForStaticProvider() {
        ProviderTokenService service = new ProviderTokenService(
                appPropertiesWithStaticProvider(),
                command -> ProviderTokenLoginResult.builder().token("unused").build(),
                fixedClock());
        ProviderTokenRefreshRequest request = new ProviderTokenRefreshRequest();
        request.setCategoryCode("1");
        request.setSubcategoryCode("1");
        request.setServiceProviderCode("1");

        assertThrows(BusinessException.class, () -> service.refreshToken(request));
    }

    private Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-04-24T16:00:00Z"), ZoneId.of("America/Guayaquil"));
    }

    private AppProperties appPropertiesWithStaticProvider() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setProviderName("ECUABET");
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");
        provider.setToken("ecuabet-static-token");
        provider.getAuth().setMode("STATIC");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("ecuabet", provider);
        return appProperties;
    }

    private AppProperties appPropertiesWithDynamicProvider() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setProviderName("LOTERIA NACIONAL");
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");
        provider.setBaseUrl("https://www8.loteria.com.ec");
        provider.getAuth().setMode("LOGIN");
        provider.getAuth().setTtlHours(24);
        provider.getAuth().getLogin().setPath("/APIVentasLoteria/api/Ventas/Login");
        provider.getAuth().getLogin().setUsername("USRFEMSAPREP");
        provider.getAuth().getLogin().setPassword("F3m993sA.");
        provider.getAuth().getLogin().setProductToSell("Bet593");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("loteria", provider);
        return appProperties;
    }

    private AppProperties appPropertiesWithGenericAndSpecificDynamicProviders() {
        AppProperties.ProviderProperties genericProvider = new AppProperties.ProviderProperties();
        genericProvider.setProviderName("LOTERIA NACIONAL");
        genericProvider.setServiceProviderCode("2");
        genericProvider.setBaseUrl("https://www8.loteria.com.ec");
        genericProvider.getAuth().setMode("LOGIN");
        genericProvider.getAuth().setTtlHours(24);
        genericProvider.getAuth().getLogin().setPath("/generic");
        genericProvider.getAuth().getLogin().setUsername("generic-user");
        genericProvider.getAuth().getLogin().setPassword("generic-password");
        genericProvider.getAuth().getLogin().setProductToSell("Generic");

        AppProperties.ProviderProperties specificProvider = new AppProperties.ProviderProperties();
        specificProvider.setProviderName("LOTERIA NACIONAL");
        specificProvider.setCategoryCode("1");
        specificProvider.setSubcategoryCode("2");
        specificProvider.setServiceProviderCode("2");
        specificProvider.setBaseUrl("https://www8.loteria.com.ec");
        specificProvider.getAuth().setMode("LOGIN");
        specificProvider.getAuth().setTtlHours(24);
        specificProvider.getAuth().getLogin().setPath("/specific");
        specificProvider.getAuth().getLogin().setUsername("specific-user");
        specificProvider.getAuth().getLogin().setPassword("specific-password");
        specificProvider.getAuth().getLogin().setProductToSell("Specific");

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().getProviders().put("loteria-generic", genericProvider);
        appProperties.getIntegration().getProviders().put("loteria-specific", specificProvider);
        return appProperties;
    }
}
