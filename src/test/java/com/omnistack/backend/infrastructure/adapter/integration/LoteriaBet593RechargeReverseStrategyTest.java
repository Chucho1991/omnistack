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
import com.omnistack.backend.application.port.out.Bet593RechargeReversePort;
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
class LoteriaBet593RechargeReverseStrategyTest {

    @Mock
    private Bet593RechargeReversePort bet593RechargeReversePort;

    private LoteriaBet593RechargeReverseStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("100708850");
        capabilityProperties.getCashin().setPath("/APIVentasLoteria/api/Ventas/ReversarRetiroBet593");
        capabilityProperties.getCashin().setCapabilities("REVRETIROOL");
        capabilityProperties.getCashin().setName("REVRETIROOL");
        provider.getServices().put("REVERSE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593RechargeReverseStrategy(bet593RechargeReversePort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashinReverse() {
        ServiceDefinition cashinService = serviceDefinition(MovementType.CASH_IN, "100708850");
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "100708848");

        assertTrue(strategy.supports(cashinService, Capability.REVERSE));
        assertFalse(strategy.supports(cashinService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashoutService, Capability.REVERSE));
    }

    @Test
    void shouldBuildReverseCommandAndReturnInternalErrorResponse() {
        when(bet593RechargeReversePort.reverseRecharge(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(false)
                .externalCode("400066")
                .externalMessage("Tiempo excedido para realizar un reverso de recarga")
                .payload(Map.of(
                        "error", 1,
                        "message", "Tiempo excedido para realizar un reverso de recarga",
                        "document", "0901111112"))
                .build());

        ReverseRequest request = reverseRequestBuilder().build();

        ReverseResponse response = (ReverseResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_IN, "100708850"),
                Capability.REVERSE);

        ArgumentCaptor<Bet593RechargeCommand> captor = ArgumentCaptor.forClass(Bet593RechargeCommand.class);
        verify(bet593RechargeReversePort).reverseRecharge(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ReversarRetiroBet593"));

        assertEquals("ca9b201a-a668-45ed-876c-00affcb18580", captor.getValue().getUuid());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("Demora en obtener respuesta", captor.getValue().getMotivo());
        assertTrue(response.isErrorFlag());
        assertEquals("400066", response.getError().getCode());
        assertEquals("Tiempo excedido para realizar un reverso de recarga", response.getError().getMessage());
        assertEquals("0901111112", response.getDocument());
    }

    @Test
    void shouldFailWhenMotivoIsMissing() {
        ReverseRequest request = reverseRequestBuilder()
                .motivo(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "100708850"), Capability.REVERSE));
    }

    private ReverseRequest.ReverseRequestBuilder<?, ?> reverseRequestBuilder() {
        return ReverseRequest.builder()
                .uuid("ca9b201a-a668-45ed-876c-00affcb18580")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("100708850")
                .authorization("4E26639D-DB2E-4E07-90E0-7C2B16DDA5FE")
                .serialnumber("serial-1")
                .document("0901111112")
                .motivo("Demora en obtener respuesta")
                .amount(new BigDecimal("200.00"));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.REVERSE))
                .build();
    }
}
