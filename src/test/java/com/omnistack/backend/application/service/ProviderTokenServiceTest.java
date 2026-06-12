package com.omnistack.backend.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.port.out.ProviderTokenLoginPort;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;
import com.omnistack.backend.shared.exception.BusinessException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ProviderTokenServiceTest {

    @Test
    void shouldReturnConfiguredStaticToken() {
        ProviderTokenService service = new ProviderTokenService(
                providerConfigServiceWithStaticProvider(),
                command -> ProviderTokenLoginResult.builder().token("unused").build(),
                Mockito.mock(ProviderWsService.class),
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
                providerConfigServiceWithDynamicProvider(),
                loginPort,
                Mockito.mock(ProviderWsService.class),
                fixedClock());

        String firstToken = service.getToken("1", "1", "2");
        String secondToken = service.getToken("1", "1", "2");

        assertEquals("dynamic-token-1", firstToken);
        assertEquals("dynamic-token-1", secondToken);
        assertEquals(1, loginCalls.get());
    }

    @Test
    void shouldForceRefreshDynamicTokenEvenWhenCachedTokenIsStillValid() {
        AtomicInteger loginCalls = new AtomicInteger();
        ProviderTokenLoginPort loginPort = command -> ProviderTokenLoginResult.builder()
                .token("dynamic-token-" + loginCalls.incrementAndGet())
                .build();
        ProviderWsService providerWsService = Mockito.mock(ProviderWsService.class);
        Mockito.when(providerWsService.requireUrl(Mockito.anyString(), Mockito.eq("LOGIN"), Mockito.anyString()))
                .thenReturn("http://mock-login-url");
        ProviderTokenService service = new ProviderTokenService(
                providerConfigServiceWithDynamicProvider(),
                loginPort,
                providerWsService,
                fixedClock());

        String firstToken = service.getToken("1", "1", "2");
        String refreshedToken = service.refreshToken("1", "1", "2");
        String cachedToken = service.getToken("1", "1", "2");

        assertEquals("dynamic-token-1", firstToken);
        assertEquals("dynamic-token-2", refreshedToken);
        assertEquals("dynamic-token-2", cachedToken);
        assertEquals(2, loginCalls.get());
    }

    @Test
    void shouldPreferMostSpecificConfigurationForContext() {
        AtomicInteger loginCalls = new AtomicInteger();
        ProviderTokenLoginPort loginPort = command -> ProviderTokenLoginResult.builder()
                .token(command.getCategoryCode() + "-" + command.getSubcategoryCode() + "-" + loginCalls.incrementAndGet())
                .build();
        ProviderTokenService service = new ProviderTokenService(
                providerConfigServiceWithGenericAndSpecificDynamicProviders(),
                loginPort,
                Mockito.mock(ProviderWsService.class),
                fixedClock());

        String token = service.getToken("1", "2", "2");

        assertEquals("1-2-1", token);
    }

    @Test
    void shouldRejectManualRefreshForStaticProvider() {
        ProviderTokenService service = new ProviderTokenService(
                providerConfigServiceWithStaticProvider(),
                command -> ProviderTokenLoginResult.builder().token("unused").build(),
                Mockito.mock(ProviderWsService.class),
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

    private ProviderConfigService providerConfigServiceWithStaticProvider() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setProviderName("ECUABET");
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");
        provider.setToken("ecuabet-static-token");
        provider.getAuth().setMode("STATIC");

        ProviderConfigService configService = Mockito.mock(ProviderConfigService.class);
        Mockito.when(configService.allProviderKeys()).thenReturn(Set.of("ecuabet"));
        Mockito.when(configService.getProviderProperties("ecuabet")).thenReturn(provider);
        return configService;
    }

    private ProviderConfigService providerConfigServiceWithDynamicProvider() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setProviderName("LOTERIA NACIONAL");
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");
        provider.getAuth().setMode("LOGIN");
        provider.getAuth().setTtlHours(24);
        provider.getAuth().getLogin().setUsername("USRFEMSAPREP");
        provider.getAuth().getLogin().setPassword("F3m993sA.");
        provider.getAuth().getLogin().setProductToSell("Bet593");

        ProviderConfigService configService = Mockito.mock(ProviderConfigService.class);
        Mockito.when(configService.allProviderKeys()).thenReturn(Set.of("loteria"));
        Mockito.when(configService.getProviderProperties("loteria")).thenReturn(provider);
        return configService;
    }

    private ProviderConfigService providerConfigServiceWithGenericAndSpecificDynamicProviders() {
        AppProperties.ProviderProperties genericProvider = new AppProperties.ProviderProperties();
        genericProvider.setProviderName("LOTERIA NACIONAL");
        genericProvider.setServiceProviderCode("2");
        genericProvider.getAuth().setMode("LOGIN");
        genericProvider.getAuth().setTtlHours(24);
        genericProvider.getAuth().getLogin().setUsername("generic-user");
        genericProvider.getAuth().getLogin().setPassword("generic-password");
        genericProvider.getAuth().getLogin().setProductToSell("Generic");

        AppProperties.ProviderProperties specificProvider = new AppProperties.ProviderProperties();
        specificProvider.setProviderName("LOTERIA NACIONAL");
        specificProvider.setCategoryCode("1");
        specificProvider.setSubcategoryCode("2");
        specificProvider.setServiceProviderCode("2");
        specificProvider.getAuth().setMode("LOGIN");
        specificProvider.getAuth().setTtlHours(24);
        specificProvider.getAuth().getLogin().setUsername("specific-user");
        specificProvider.getAuth().getLogin().setPassword("specific-password");
        specificProvider.getAuth().getLogin().setProductToSell("Specific");

        ProviderConfigService configService = Mockito.mock(ProviderConfigService.class);
        Mockito.when(configService.allProviderKeys()).thenReturn(Set.of("loteria-generic", "loteria-specific"));
        Mockito.when(configService.getProviderProperties("loteria-generic")).thenReturn(genericProvider);
        Mockito.when(configService.getProviderProperties("loteria-specific")).thenReturn(specificProvider);
        return configService;
    }
}
