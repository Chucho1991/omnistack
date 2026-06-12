package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.port.out.EcuabetWithdrawReversePort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EcuabetWithdrawReverseStrategyTest {

    @Mock
    private EcuabetWithdrawReversePort ecuabetWithdrawReversePort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private EcuabetWithdrawReverseStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");

        when(providerConfigService.getProviderProperties("ecuabet")).thenReturn(provider);
        when(providerWsDefsService.getString("ecuabet", "REVERSE.CASHOUT", "item")).thenReturn("100708846");

        when(providerWsService.hasUrl("ecuabet", "REVERSE.CASHOUT")).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn("/rollback/withdraw");

        strategy = new EcuabetWithdrawReverseStrategy(ecuabetWithdrawReversePort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredEcuabetCashoutReverse() {
        ServiceDefinition serviceDefinition = serviceDefinition(MovementType.CASH_OUT, "100708846");

        assertTrue(strategy.supports(serviceDefinition, Capability.REVERSE));
        assertFalse(strategy.supports(serviceDefinition, Capability.EXECUTE));
        assertFalse(strategy.supports(serviceDefinition(MovementType.CASH_IN, "100713841"), Capability.REVERSE));
    }

    @Test
    void shouldBuildReverseWithdrawCommandAndReturnGeneratedAuthorization() {
        when(ecuabetWithdrawReversePort.reverseWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("Transaccion correcta")
                .payload(Map.of(
                        "error", 0,
                        "code", "0",
                        "amount", new BigDecimal("25.5000"),
                        "authorization", "41472",
                        "providerTransactionId", "41472"))
                .build());

        ReverseRequest request = ReverseRequest.builder()
                .uuid("uuid-cashout-reverse")
                .chain("1")
                .store("148")
                .storeName("FYBECA EL BATAN")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .withdrawId("7671")
                .password("03448")
                .document("0912345678")
                .amount(new BigDecimal("25.50"))
                .authorization("41472")
                .motivo("Reverso de nota de retiro")
                .build();

        ReverseResponse response = (ReverseResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708846"),
                Capability.REVERSE);

        ArgumentCaptor<EcuabetWithdrawCommand> captor = ArgumentCaptor.forClass(EcuabetWithdrawCommand.class);
        verify(ecuabetWithdrawReversePort).reverseWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq("/rollback/withdraw"));

        assertEquals("7671", captor.getValue().getWithdrawId());
        assertEquals("03448", captor.getValue().getPassword());
        assertEquals(new BigDecimal("25.50"), captor.getValue().getAmount());
        assertNotNull(captor.getValue().getTransactionId());
        assertEquals("41472", response.getAuthorization());
        assertEquals("0912345678", response.getDocument());
        assertEquals(new BigDecimal("25.5000"), response.getAmount());
        assertEquals("00", response.getStatus().getCode());
    }

    @Test
    void shouldFailWhenPasswordIsMissing() {
        ReverseRequest request = ReverseRequest.builder()
                .uuid("uuid-cashout-reverse")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .withdrawId("7671")
                .amount(new BigDecimal("25.50"))
                .motivo("Reverso de nota de retiro")
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "100708846"), Capability.REVERSE));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode(rmsItemCode)
                .description("ECUABET CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK, Capability.EXECUTE, Capability.REVERSE))
                .build();
    }
}
