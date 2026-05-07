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
import com.omnistack.backend.application.port.out.EcuabetDepositPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetDepositCommand;
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
class EcuabetDepositExecuteStrategyTest {

    @Mock
    private EcuabetDepositPort ecuabetDepositPort;

    private EcuabetDepositExecuteStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("1");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("100713841");
        capabilityProperties.getCashin().setPath("/user/deposit");
        capabilityProperties.getCashin().setCapabilities("DEPOSITO");
        capabilityProperties.getCashin().setName("DEPOSITO");
        provider.getServices().put("EXECUTE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("ecuabet", provider)));

        strategy = new EcuabetDepositExecuteStrategy(ecuabetDepositPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredEcuabetCashinExecute() {
        ServiceDefinition serviceDefinition = serviceDefinition(MovementType.CASH_IN, "100713841");

        assertTrue(strategy.supports(serviceDefinition, Capability.EXECUTE));
        assertFalse(strategy.supports(serviceDefinition, Capability.PRECHECK));
        assertFalse(strategy.supports(serviceDefinition(MovementType.CASH_OUT, "100708846"), Capability.EXECUTE));
    }

    @Test
    void shouldBuildDepositCommandAndReturnProviderAuthorization() {
        when(ecuabetDepositPort.deposit(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
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
                        "amount", new BigDecimal("99999.00")))
                .build());

        ExecuteRequest request = ExecuteRequest.builder()
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
                .build();

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_IN, "100713841"),
                Capability.EXECUTE);

        ArgumentCaptor<EcuabetDepositCommand> captor = ArgumentCaptor.forClass(EcuabetDepositCommand.class);
        verify(ecuabetDepositPort).deposit(captor.capture(), org.mockito.ArgumentMatchers.eq("/user/deposit"));

        assertEquals("997561", captor.getValue().getUserid());
        assertEquals(new BigDecimal("100000.00"), captor.getValue().getAmount());
        assertNotNull(captor.getValue().getTransactionId());
        assertEquals("91081", response.getAuthorization());
        assertEquals("Carlos", response.getUsername());
        assertEquals("Perez", response.getLastname());
        assertEquals("USD", response.getCurrency());
        assertEquals("0912345678", response.getDocument());
        assertEquals(new BigDecimal("99999.00"), response.getAmount());
        assertEquals("0", response.getStatus().getCode());
    }

    @Test
    void shouldFailWhenCategoryDoesNotMatchConfiguration() {
        ExecuteRequest request = ExecuteRequest.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("9")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .userid("997561")
                .amount(new BigDecimal("100000.00"))
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "100713841"), Capability.EXECUTE));
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
