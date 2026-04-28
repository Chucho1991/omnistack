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
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EcuabetWithdrawReverseStrategyTest {

    @Mock
    private EcuabetWithdrawReversePort ecuabetWithdrawReversePort;

    private EcuabetWithdrawReverseStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("10001565827");
        capabilityProperties.getCashout().setPath("/rollback/withdraw");
        capabilityProperties.getCashout().setCapabilities("REVERSO_NOTA_RETIRO");
        capabilityProperties.getCashout().setName("REVERSO_NOTA_RETIRO");
        provider.getServices().put("REVERSE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("ecuabet", provider)));

        strategy = new EcuabetWithdrawReverseStrategy(ecuabetWithdrawReversePort, appProperties);
    }

    @Test
    void shouldSupportConfiguredEcuabetCashoutReverse() {
        ServiceDefinition serviceDefinition = serviceDefinition(MovementType.CASH_OUT, "10001565827");

        assertTrue(strategy.supports(serviceDefinition, Capability.REVERSE));
        assertFalse(strategy.supports(serviceDefinition, Capability.EXECUTE));
        assertFalse(strategy.supports(serviceDefinition(MovementType.CASH_IN, "10001565826"), Capability.REVERSE));
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
                        "amount", new BigDecimal("20.00"),
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
                .rmsItemCode("10001565827")
                .withdrawId("7671")
                .password("03448")
                .document("0912345678")
                .amount(new BigDecimal("25.50"))
                .authorization("original-auth")
                .motivo("Reverso de nota de retiro")
                .build();

        ReverseResponse response = (ReverseResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "10001565827"),
                Capability.REVERSE);

        ArgumentCaptor<EcuabetWithdrawCommand> captor = ArgumentCaptor.forClass(EcuabetWithdrawCommand.class);
        verify(ecuabetWithdrawReversePort).reverseWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq("/rollback/withdraw"));

        assertEquals("7671", captor.getValue().getWithdrawId());
        assertEquals("03448", captor.getValue().getPassword());
        assertEquals(new BigDecimal("25.50"), captor.getValue().getAmount());
        assertNotNull(captor.getValue().getTransactionId());
        assertEquals(String.valueOf(captor.getValue().getTransactionId()), response.getAuthorization());
        assertEquals("0912345678", response.getDocument());
        assertEquals(new BigDecimal("20.00"), response.getAmount());
        assertEquals("0", response.getStatus().getCode());
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
                .rmsItemCode("10001565827")
                .withdrawId("7671")
                .amount(new BigDecimal("25.50"))
                .motivo("Reverso de nota de retiro")
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "10001565827"), Capability.REVERSE));
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
