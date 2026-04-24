package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.port.out.Bet593RechargePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593RechargeCommand;
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
class LoteriaBet593PrecheckStrategyTest {

    @Mock
    private Bet593RechargePort bet593RechargePort;

    private LoteriaBet593PrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("10001565828");
        capabilityProperties.getCashin().setPath("/APIVentasLoteria/api/Ventas/RecargarBet593");
        capabilityProperties.getCashin().setCapabilities("RECARGA593");
        capabilityProperties.getCashin().setName("RECARGA593");
        provider.getServices().put("PRECHECK", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593PrecheckStrategy(bet593RechargePort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashinPrecheck() {
        ServiceDefinition bet593Service = serviceDefinition(MovementType.CASH_IN, "10001565828");
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "10001565829");

        assertTrue(strategy.supports(bet593Service, Capability.PRECHECK));
        assertFalse(strategy.supports(bet593Service, Capability.EXECUTE));
        assertFalse(strategy.supports(cashoutService, Capability.PRECHECK));
    }

    @Test
    void shouldBuildRechargeCommandAndReturnInternalResponse() {
        when(bet593RechargePort.recharge(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "name", "Usuario",
                        "lastname", "Prueba uno",
                        "authorization", "4E26639D-DB2E-4E07-90E0-7C2B16DDA5FE",
                        "serialnumber", "serial-1",
                        "document", "0901111112",
                        "amount", "9.99"))
                .build());

        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-bet593")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565828")
                .document("0901111112")
                .amount(new BigDecimal("9.99"))
                .build();

        var response = strategy.process(request, serviceDefinition(MovementType.CASH_IN, "10001565828"), Capability.PRECHECK);

        ArgumentCaptor<Bet593RechargeCommand> captor = ArgumentCaptor.forClass(Bet593RechargeCommand.class);
        verify(bet593RechargePort).recharge(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/RecargarBet593"));

        assertEquals("uuid-bet593", captor.getValue().getUuid());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals(new BigDecimal("9.99"), captor.getValue().getAmount());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Usuario", ((com.omnistack.backend.application.dto.PrecheckResponse) response).getUsername());
        assertEquals("Prueba uno", ((com.omnistack.backend.application.dto.PrecheckResponse) response).getLastname());
    }

    @Test
    void shouldFailWhenCategoryDoesNotMatchConfiguration() {
        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-bet593")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .categoryCode("9")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565828")
                .document("0901111112")
                .amount(new BigDecimal("9.99"))
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "10001565828"), Capability.PRECHECK));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }
}
