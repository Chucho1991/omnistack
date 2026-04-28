package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.application.port.out.Bet593WithdrawValidationPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
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
class LoteriaBet593WithdrawVerifyStrategyTest {

    @Mock
    private Bet593WithdrawValidationPort bet593WithdrawValidationPort;

    private LoteriaBet593WithdrawVerifyStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("10001565829");
        capabilityProperties.getCashout().setPath("/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593");
        capabilityProperties.getCashout().setCapabilities("CONRETIROOL");
        capabilityProperties.getCashout().setName("CONRETIROOL");
        provider.getServices().put("VERIFY", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593WithdrawVerifyStrategy(bet593WithdrawValidationPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashoutVerify() {
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "10001565829");
        ServiceDefinition cashinService = serviceDefinition(MovementType.CASH_IN, "10001565828");

        assertTrue(strategy.supports(cashoutService, Capability.VERIFY));
        assertFalse(strategy.supports(cashoutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashinService, Capability.VERIFY));
    }

    @Test
    void shouldBuildWithdrawValidationCommandAndReturnInternalResponse() {
        when(bet593WithdrawValidationPort.validateWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "message", "",
                        "document", "0901111112"))
                .build());

        VerifyRequest request = verifyRequestBuilder().build();

        VerifyResponse response = (VerifyResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "10001565829"),
                Capability.VERIFY);

        ArgumentCaptor<Bet593WithdrawCommand> captor = ArgumentCaptor.forClass(Bet593WithdrawCommand.class);
        verify(bet593WithdrawValidationPort).validateWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593"));

        assertEquals("uuid-bet593-cashout-verify", captor.getValue().getUuid());
        assertEquals("340468406359", captor.getValue().getWithdrawId());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
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

        VerifyResponse response = (VerifyResponse) strategy.process(
                verifyRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "10001565829"),
                Capability.VERIFY);

        assertFalse(response.isErrorFlag());
        assertEquals("400022", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
    }

    @Test
    void shouldFailWhenWithdrawIdIsMissing() {
        VerifyRequest request = verifyRequestBuilder()
                .withdrawId(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "10001565829"), Capability.VERIFY));
    }

    private VerifyRequest.VerifyRequestBuilder<?, ?> verifyRequestBuilder() {
        return VerifyRequest.builder()
                .uuid("uuid-bet593-cashout-verify")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565829")
                .withdrawId("340468406359")
                .document("0901111112");
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.VERIFY))
                .build();
    }
}
