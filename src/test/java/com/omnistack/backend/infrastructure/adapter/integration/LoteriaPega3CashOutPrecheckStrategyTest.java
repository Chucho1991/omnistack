package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.port.out.Pega3VerifyTicketPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.BusinessException;
import com.omnistack.backend.shared.exception.IntegrationException;
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
class LoteriaPega3CashOutPrecheckStrategyTest {

    private static final String WS_KEY = "PRECHECK.CASHOUT";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/ConsultarTicket";

    @Mock
    private Pega3VerifyTicketPort pega3VerifyTicketPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private LoteriaPega3CashOutPrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("2");
        provider.setServiceProviderCode("2");

        when(providerConfigService.getProviderProperties("pega3")).thenReturn(provider);
        when(providerWsDefsService.getString("pega3", WS_KEY, "item")).thenReturn("100708862");
        when(providerWsService.hasUrl("pega3", WS_KEY)).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn(OPERATION_URL);

        strategy = new LoteriaPega3CashOutPrecheckStrategy(
                pega3VerifyTicketPort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredPega3CashOutPrecheck() {
        ServiceDefinition cashOutService = serviceDefinition(MovementType.CASH_OUT, "100708862");
        ServiceDefinition cashInService = serviceDefinition(MovementType.CASH_IN, "100708852");

        assertTrue(strategy.supports(cashOutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashOutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashInService, Capability.PRECHECK));
    }

    @Test
    void shouldReturnPrecheckResponseWhenTicketIsWinner() {
        when(pega3VerifyTicketPort.verifyTicket(any(), anyString(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "authorization", "TO0120002171594310046039",
                        "ticket_status", "Purchased",
                        "is_winner", true,
                        "prize_amount", "200.00"))
                .build());

        PrecheckResponse response = (PrecheckResponse) strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.PRECHECK);

        assertFalse(response.isErrorFlag());
        assertTrue(response.getWinner());
        assertEquals("200.00", response.getPrizeAmount().toPlainString());
        assertEquals("TO0120002171594310046039", response.getAuthorization());
    }

    @Test
    void shouldThrowBusinessExceptionWhenTicketHasNoPrize() {
        when(pega3VerifyTicketPort.verifyTicket(any(), anyString(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "ticket_status", "No Winning",
                        "is_winner", false))
                .build());

        assertThrows(BusinessException.class, () -> strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.PRECHECK));
    }

    @Test
    void shouldFailWhenAuthorizationIsMissing() {
        PrecheckRequest request = precheckRequestBuilder().authorization(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708862"),
                Capability.PRECHECK));
    }

    private PrecheckRequest.PrecheckRequestBuilder<?, ?> precheckRequestBuilder() {
        return PrecheckRequest.builder()
                .uuid("uuid-pega3-cashout-precheck")
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
                .authorization("TO0120002171594310046039");
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("PEGA3 CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }
}
