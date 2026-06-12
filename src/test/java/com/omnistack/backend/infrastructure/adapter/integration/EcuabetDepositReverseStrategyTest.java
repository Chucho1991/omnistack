package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.port.out.EcuabetDepositReversePort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetDepositCommand;
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
class EcuabetDepositReverseStrategyTest {

    @Mock
    private EcuabetDepositReversePort ecuabetDepositReversePort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private EcuabetDepositReverseStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");

        when(providerConfigService.getProviderProperties("ecuabet")).thenReturn(provider);
        when(providerWsDefsService.getString("ecuabet", "REVERSE.CASHIN", "item")).thenReturn("100713841");

        when(providerWsService.hasUrl("ecuabet", "REVERSE.CASHIN")).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn("/rollback/deposit");

        strategy = new EcuabetDepositReverseStrategy(ecuabetDepositReversePort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredEcuabetCashinReverse() {
        ServiceDefinition serviceDefinition = serviceDefinition(MovementType.CASH_IN, "100713841");

        assertTrue(strategy.supports(serviceDefinition, Capability.REVERSE));
        assertFalse(strategy.supports(serviceDefinition, Capability.EXECUTE));
        assertFalse(strategy.supports(serviceDefinition(MovementType.CASH_OUT, "100708846"), Capability.REVERSE));
    }

    @Test
    void shouldBuildReverseCommandAndReturnOriginalAuthorization() {
        when(ecuabetDepositReversePort.reverseDeposit(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("Transaccion correcta")
                .payload(Map.of(
                        "error", 0,
                        "code", "0",
                        "name", "Carlos",
                        "lastname", "Perez",
                        "currency", "USD",
                        "authorization", "91081",
                        "providerTransactionId", "41472",
                        "amount", new BigDecimal("100000.00")))
                .build());

        ReverseRequest request = ReverseRequest.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .storeName("FYBECA EL BATAN")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .userid("997561")
                .phone("123456")
                .document("0912345678")
                .amount(new BigDecimal("100000.00"))
                .authorization("91081")
                .motivo("Reverso por timeout")
                .build();

        ReverseResponse response = (ReverseResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_IN, "100713841"),
                Capability.REVERSE);

        ArgumentCaptor<EcuabetDepositCommand> captor = ArgumentCaptor.forClass(EcuabetDepositCommand.class);
        verify(ecuabetDepositReversePort).reverseDeposit(captor.capture(), org.mockito.ArgumentMatchers.eq("/rollback/deposit"));

        assertEquals(91081, captor.getValue().getTransactionId());
        assertEquals(new BigDecimal("100000.00"), captor.getValue().getAmount());
        assertEquals("91081", response.getAuthorization());
        assertEquals("Carlos", response.getUsername());
        assertEquals("Perez", response.getLastname());
        assertEquals("USD", response.getCurrency());
        assertEquals("0912345678", response.getDocument());
        assertEquals(new BigDecimal("100000.00"), response.getAmount());
        assertEquals("00", response.getStatus().getCode());
    }

    @Test
    void shouldFailWhenAuthorizationIsNotNumeric() {
        ReverseRequest request = ReverseRequest.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .amount(new BigDecimal("100000.00"))
                .authorization("AUTH-001")
                .motivo("Reverso por timeout")
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "100713841"), Capability.REVERSE));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode(rmsItemCode)
                .description("ECUABET CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK, Capability.EXECUTE, Capability.REVERSE))
                .build();
    }
}
