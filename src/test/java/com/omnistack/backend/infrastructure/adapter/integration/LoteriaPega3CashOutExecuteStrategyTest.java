package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.port.out.Pega3PayTicketPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LoteriaPega3CashOutExecuteStrategyTest {

    private static final String WS_KEY = "EXECUTE.CASHOUT";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/PagarTicket";

    @Mock
    private Pega3PayTicketPort pega3PayTicketPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private LoteriaPega3CashOutExecuteStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("2");
        provider.setServiceProviderCode("2");

        when(providerConfigService.getProviderProperties("pega3")).thenReturn(provider);
        when(providerWsDefsService.getString("pega3", WS_KEY, "item")).thenReturn("100708862");
        when(providerWsService.hasUrl("pega3", WS_KEY)).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn(OPERATION_URL);

        strategy = new LoteriaPega3CashOutExecuteStrategy(
                pega3PayTicketPort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredPega3CashOutExecute() {
        ServiceDefinition cashOutService = serviceDefinition(MovementType.CASH_OUT, "100708862");
        ServiceDefinition cashInService = serviceDefinition(MovementType.CASH_IN, "100708852");

        assertTrue(strategy.supports(cashOutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashOutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashInService, Capability.EXECUTE));
    }

    @Test
    void shouldPayTicketAndReturnExecuteResponse() {
        when(pega3PayTicketPort.payTicket(any(), anyString(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "authorization", "TO0001001216470859276716",
                        "total_claimed_amount", "200.00",
                        "claimed_on", "2026-07-12T14:17:37.085Z"))
                .build());

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                executeRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.EXECUTE);

        assertFalse(response.isErrorFlag());
        assertEquals("TO0001001216470859276716", response.getAuthorization());
        assertEquals("200.00", response.getAmount().toPlainString());
    }

    @Test
    void shouldFailWhenAuthorizationIsMissing() {
        ExecuteRequest request = executeRequestBuilder().authorization(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.EXECUTE));
    }

    @Test
    void shouldFailWhenAmountIsMissing() {
        ExecuteRequest request = executeRequestBuilder().amount(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.EXECUTE));
    }

    private ExecuteRequest.ExecuteRequestBuilder<?, ?> executeRequestBuilder() {
        return ExecuteRequest.builder()
                .uuid("uuid-pega3-cashout-execute")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode("100708862")
                .authorization("TO0120002171594310046039")
                .amount(new BigDecimal("200.00"));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("PEGA3 CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.EXECUTE))
                .build();
    }
}
