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

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.port.out.EcuabetWithdrawPort;
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
class EcuabetWithdrawExecuteStrategyTest {

    @Mock
    private EcuabetWithdrawPort ecuabetWithdrawPort;

    private EcuabetWithdrawExecuteStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("100708846");
        capabilityProperties.getCashout().setPath("/user/withdraw");
        capabilityProperties.getCashout().setCapabilities("EJECUCION_NOTA_RETIRO");
        capabilityProperties.getCashout().setName("EJECUCION_NOTA_RETIRO");
        provider.getServices().put("EXECUTE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("ecuabet", provider)));

        strategy = new EcuabetWithdrawExecuteStrategy(ecuabetWithdrawPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredEcuabetCashoutExecute() {
        ServiceDefinition serviceDefinition = serviceDefinition(MovementType.CASH_OUT, "100708846");

        assertTrue(strategy.supports(serviceDefinition, Capability.EXECUTE));
        assertFalse(strategy.supports(serviceDefinition, Capability.PRECHECK));
        assertFalse(strategy.supports(serviceDefinition(MovementType.CASH_IN, "100713841"), Capability.EXECUTE));
    }

    @Test
    void shouldBuildWithdrawCommandAndReturnProviderAuthorization() {
        when(ecuabetWithdrawPort.withdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("Transaccion correcta")
                .payload(Map.of(
                        "error", 0,
                        "code", "0",
                        "authorization", "91081",
                        "amount", new BigDecimal("20.00"),
                        "providerTransactionId", "41472"))
                .build());

        ExecuteRequest request = ExecuteRequest.builder()
                .uuid("uuid-cashout")
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
                .withdrawId("7668")
                .password("77992")
                .document("0912345678")
                .amount(new BigDecimal("25.50"))
                .build();

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708846"),
                Capability.EXECUTE);

        ArgumentCaptor<EcuabetWithdrawCommand> captor = ArgumentCaptor.forClass(EcuabetWithdrawCommand.class);
        verify(ecuabetWithdrawPort).withdraw(captor.capture(), org.mockito.ArgumentMatchers.eq("/user/withdraw"));

        assertEquals("7668", captor.getValue().getWithdrawId());
        assertEquals("77992", captor.getValue().getPassword());
        assertEquals(new BigDecimal("25.50"), captor.getValue().getAmount());
        assertNotNull(captor.getValue().getTransactionId());
        assertEquals("91081", response.getAuthorization());
        assertEquals("0912345678", response.getDocument());
        assertEquals(new BigDecimal("20.00"), response.getAmount());
        assertEquals("0", response.getStatus().getCode());
    }

    @Test
    void shouldFailWhenWithdrawIdIsMissing() {
        ExecuteRequest request = ExecuteRequest.builder()
                .uuid("uuid-cashout")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .password("77992")
                .amount(new BigDecimal("25.50"))
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "100708846"), Capability.EXECUTE));
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
