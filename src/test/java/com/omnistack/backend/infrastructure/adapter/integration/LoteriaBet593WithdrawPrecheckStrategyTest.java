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
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.port.out.Bet593WithdrawValidationPort;
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
class LoteriaBet593WithdrawPrecheckStrategyTest {

    @Mock
    private Bet593WithdrawValidationPort bet593WithdrawValidationPort;

    private LoteriaBet593WithdrawPrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("100708848");
        capabilityProperties.getCashout().setPath("/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593");
        capabilityProperties.getCashout().setCapabilities("CONRETIROOL");
        capabilityProperties.getCashout().setName("CONRETIROOL");
        provider.getServices().put("PRECHECK", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593WithdrawPrecheckStrategy(bet593WithdrawValidationPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashoutPrecheck() {
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "100708848");
        ServiceDefinition cashinService = serviceDefinition(MovementType.CASH_IN, "100708850");

        assertTrue(strategy.supports(cashoutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashoutService, Capability.VERIFY));
        assertFalse(strategy.supports(cashinService, Capability.PRECHECK));
    }

    @Test
    void shouldBuildWithdrawValidationCommandAndReturnPrecheckResponse() {
        when(bet593WithdrawValidationPort.validateWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "message", "",
                        "document", "0901111112",
                        "amount", "17.00"))
                .build());

        PrecheckResponse response = (PrecheckResponse) strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708848"),
                Capability.PRECHECK);

        ArgumentCaptor<Bet593WithdrawCommand> captor = ArgumentCaptor.forClass(Bet593WithdrawCommand.class);
        verify(bet593WithdrawValidationPort).validateWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593"));

        assertEquals("uuid-bet593-cashout-precheck", captor.getValue().getUuid());
        assertEquals("340468406359", captor.getValue().getWithdrawId());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("00", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
        assertEquals("17.00", response.getAmount().toPlainString());
    }

    @Test
    void shouldTreatExecutedWithdrawCodeAsStatus() {
        when(bet593WithdrawValidationPort.validateWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(false)
                .externalCode("400022")
                .externalMessage("Orden de pago no disponible para pago o ya esta pagada,")
                .payload(Map.of(
                        "error", 1,
                        "message", "Orden de pago no disponible para pago o ya esta pagada,"))
                .build());

        PrecheckResponse response = (PrecheckResponse) strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708848"),
                Capability.PRECHECK);

        assertFalse(response.isErrorFlag());
        assertEquals("00", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
    }

    @Test
    void shouldFailWhenWithdrawIdIsMissing() {
        PrecheckRequest request = precheckRequestBuilder()
                .withdrawId(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "100708848"), Capability.PRECHECK));
    }

    private PrecheckRequest.PrecheckRequestBuilder<?, ?> precheckRequestBuilder() {
        return PrecheckRequest.builder()
                .uuid("uuid-bet593-cashout-precheck")
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
                .withdrawId("340468406359")
                .document("0901111112")
                .amount(new BigDecimal("17.00"));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }
}
