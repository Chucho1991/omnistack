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
class LoteriaBet593ExecuteStrategyTest {

    @Mock
    private Bet593RechargePort bet593RechargePort;

    private LoteriaBet593ExecuteStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("100708850");
        capabilityProperties.getCashin().setPath("/APIVentasLoteria/api/Ventas/ConfirmarBet593");
        capabilityProperties.getCashin().setCapabilities("CONFIRMA593");
        capabilityProperties.getCashin().setName("CONFIRMA593");
        provider.getServices().put("EXECUTE", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593ExecuteStrategy(bet593RechargePort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashinExecute() {
        ServiceDefinition bet593Service = serviceDefinition(MovementType.CASH_IN, "100708850");
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "100708848");

        assertTrue(strategy.supports(bet593Service, Capability.EXECUTE));
        assertFalse(strategy.supports(bet593Service, Capability.PRECHECK));
        assertFalse(strategy.supports(cashoutService, Capability.EXECUTE));
    }

    @Test
    void shouldBuildConfirmationCommandAndReturnInternalResponse() {
        when(bet593RechargePort.recharge(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "name", "Usuario",
                        "lastname", "Prueba uno",
                        "authorization", "9F968187-F436-4F19-8C1F-A7A4DA07A899",
                        "serialnumber", "7366ea56284a06a2",
                        "document", "0901111112",
                        "amount", "9.99"))
                .build());

        ExecuteRequest request = executeRequestBuilder()
                .build();

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_IN, "100708850"),
                Capability.EXECUTE);

        ArgumentCaptor<Bet593RechargeCommand> captor = ArgumentCaptor.forClass(Bet593RechargeCommand.class);
        verify(bet593RechargePort).recharge(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ConfirmarBet593"));

        assertEquals("uuid-bet593", captor.getValue().getUuid());
        assertEquals("9F968187-F436-4F19-8C1F-A7A4DA07A899", captor.getValue().getAuthorization());
        assertEquals("7366ea56284a06a2", captor.getValue().getSerialnumber());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals(new BigDecimal("9.99"), captor.getValue().getAmount());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Usuario", response.getUsername());
        assertEquals("Prueba uno", response.getLastname());
        assertEquals("9F968187-F436-4F19-8C1F-A7A4DA07A899", response.getAuthorization());
        assertEquals("7366ea56284a06a2", response.getSerialnumber());
    }

    @Test
    void shouldFailWhenConfirmationFieldsAreMissing() {
        ExecuteRequest request = executeRequestBuilder()
                .authorization(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "100708850"), Capability.EXECUTE));
    }

    private ExecuteRequest.ExecuteRequestBuilder<?, ?> executeRequestBuilder() {
        return ExecuteRequest.builder()
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
                .rmsItemCode("100708850")
                .authorization("9F968187-F436-4F19-8C1F-A7A4DA07A899")
                .serialnumber("7366ea56284a06a2")
                .document("0901111112")
                .amount(new BigDecimal("9.99"));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.EXECUTE))
                .build();
    }
}
