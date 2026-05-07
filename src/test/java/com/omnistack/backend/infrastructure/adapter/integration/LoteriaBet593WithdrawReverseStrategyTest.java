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
import com.omnistack.backend.application.port.out.Bet593WithdrawReversePort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
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
class LoteriaBet593WithdrawReverseStrategyTest {

    @Mock
    private Bet593WithdrawReversePort bet593WithdrawReversePort;

    private LoteriaBet593WithdrawReverseStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("100708848");
        capabilityProperties.getCashout().setPath("/APIVentasLoteria/api/Ventas/ReversarRetiroBet593");
        capabilityProperties.getCashout().setCapabilities("REVRETIROOL");
        capabilityProperties.getCashout().setName("REVRETIROOL");
        provider.getServices().put("REVERSE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593WithdrawReverseStrategy(bet593WithdrawReversePort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashoutReverse() {
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "100708848");
        ServiceDefinition cashinService = serviceDefinition(MovementType.CASH_IN, "100708850");

        assertTrue(strategy.supports(cashoutService, Capability.REVERSE));
        assertFalse(strategy.supports(cashoutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashinService, Capability.REVERSE));
    }

    @Test
    void shouldBuildReverseCommandAndReturnInternalResponse() {
        when(bet593WithdrawReversePort.reverseWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "message", "",
                        "transactionNumber", "ca9b201a-a668-45ed-876c-00affcb18580",
                        "document", "0901111112"))
                .build());

        ReverseRequest request = reverseRequestBuilder().build();

        ReverseResponse response = (ReverseResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708848"),
                Capability.REVERSE);

        ArgumentCaptor<Bet593WithdrawCommand> captor = ArgumentCaptor.forClass(Bet593WithdrawCommand.class);
        verify(bet593WithdrawReversePort).reverseWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ReversarRetiroBet593"));

        assertEquals("uuid-bet593-reverse", captor.getValue().getUuid());
        assertEquals("ca9b201a-a668-45ed-876c-00affcb18580", captor.getValue().getAuthorization());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("Demora en obtener respuesta", captor.getValue().getMotivo());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Transacci\u00F3n correcta", response.getStatus().getMessage());
        assertEquals("ca9b201a-a668-45ed-876c-00affcb18580", response.getAuthorization());
        assertEquals("0901111112", response.getDocument());
    }

    @Test
    void shouldFailWhenAuthorizationIsMissing() {
        ReverseRequest request = reverseRequestBuilder()
                .authorization(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "100708848"), Capability.REVERSE));
    }

    private ReverseRequest.ReverseRequestBuilder<?, ?> reverseRequestBuilder() {
        return ReverseRequest.builder()
                .uuid("uuid-bet593-reverse")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("100708848")
                .withdrawId("82319")
                .authorization("ca9b201a-a668-45ed-876c-00affcb18580")
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
                .description("BET 593 CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.REVERSE))
                .build();
    }
}
