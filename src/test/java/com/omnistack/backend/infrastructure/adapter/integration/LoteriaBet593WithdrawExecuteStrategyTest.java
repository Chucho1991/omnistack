package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.port.out.Bet593WithdrawPort;
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
class LoteriaBet593WithdrawExecuteStrategyTest {

    @Mock
    private Bet593WithdrawPort bet593WithdrawPort;

    private LoteriaBet593WithdrawExecuteStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashout().setItem("10001565829");
        capabilityProperties.getCashout().setPath("/APIVentasLoteria/api/Ventas/RetirarBet593");
        capabilityProperties.getCashout().setCapabilities("RETIROOL");
        capabilityProperties.getCashout().setName("RETIROOL");
        provider.getServices().put("EXECUTE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593WithdrawExecuteStrategy(bet593WithdrawPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashoutExecute() {
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "10001565829");
        ServiceDefinition cashinService = serviceDefinition(MovementType.CASH_IN, "10001565828");

        assertTrue(strategy.supports(cashoutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashoutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashinService, Capability.EXECUTE));
    }

    @Test
    void shouldBuildWithdrawCommandAndReturnInternalResponse() {
        when(bet593WithdrawPort.withdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "message", "",
                        "authorization", 71787,
                        "document", "0911274165",
                        "amount", "17.000000"))
                .build());

        ExecuteRequest request = executeRequestBuilder().build();

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "10001565829"),
                Capability.EXECUTE);

        ArgumentCaptor<Bet593WithdrawCommand> captor = ArgumentCaptor.forClass(Bet593WithdrawCommand.class);
        verify(bet593WithdrawPort).withdraw(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/RetirarBet593"));

        assertEquals("uuid-bet593-cashout", captor.getValue().getUuid());
        assertEquals("20240430800100007", captor.getValue().getWithdrawId());
        assertEquals("0911274165", captor.getValue().getDocument());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("71787", response.getAuthorization());
        assertEquals("0911274165", response.getDocument());
        assertEquals(new BigDecimal("17.000000"), response.getAmount());
    }

    @Test
    void shouldFailWhenWithdrawIdIsMissing() {
        ExecuteRequest request = executeRequestBuilder()
                .withdrawId(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "10001565829"), Capability.EXECUTE));
    }

    private ExecuteRequest.ExecuteRequestBuilder<?, ?> executeRequestBuilder() {
        return ExecuteRequest.builder()
                .uuid("uuid-bet593-cashout")
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
                .withdrawId("20240430800100007")
                .document("0911274165")
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
                .capabilities(List.of(Capability.EXECUTE))
                .build();
    }
}
