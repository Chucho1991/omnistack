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
import com.omnistack.backend.application.port.out.TradicionalConsultarTicketPort;
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
class LoteriaTradicionalCashOutPrecheckStrategyTest {

    private static final String WS_KEY = "PRECHECK.CASHOUT";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/ConsultarTicket";

    @Mock
    private TradicionalConsultarTicketPort consultarTicketPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private LoteriaTradicionalCashOutPrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("2");
        provider.setServiceProviderCode("2");
        provider.setClienteId(46251);
        provider.getAuth().getLogin().setUsername("usrtiaprep");

        when(providerConfigService.getProviderProperties("tradicional")).thenReturn(provider);
        when(providerWsDefsService.getString("tradicional", WS_KEY, "item")).thenReturn("100708854");
        when(providerWsService.hasUrl("tradicional", WS_KEY)).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn(OPERATION_URL);

        strategy = new LoteriaTradicionalCashOutPrecheckStrategy(
                consultarTicketPort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredTradicionalCashOutPrecheck() {
        ServiceDefinition cashOutService = serviceDefinition(MovementType.CASH_OUT, "100708854");
        ServiceDefinition cashInService = serviceDefinition(MovementType.CASH_IN, "100713842");

        assertTrue(strategy.supports(cashOutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashOutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashInService, Capability.PRECHECK));
    }

    @Test
    void shouldReturnPrecheckResponseWhenBoletoIsWinner() {
        when(consultarTicketPort.consultarTicket(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "mpi", "957",
                        "is_winner", true,
                        "ticket_status", "",
                        "prize_amount", "1000000.00",
                        "authorization", "664071071637"))
                .build());

        PrecheckResponse response = (PrecheckResponse) strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.PRECHECK);

        assertFalse(response.isErrorFlag());
        assertTrue(response.getWinner());
        assertEquals("957", response.getMpi());
        assertEquals("1000000.00", response.getPrizeAmount().toPlainString());
        assertEquals("664071071637", response.getAuthorization());
    }

    @Test
    void shouldThrowBusinessExceptionWhenBoletoHasNoPrize() {
        when(consultarTicketPort.consultarTicket(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "is_winner", false,
                        "ticket_status", "Boleto no tiene premio."))
                .build());

        assertThrows(BusinessException.class, () -> strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.PRECHECK));
    }

    @Test
    void shouldFailWhenAuthorizationIsMissing() {
        PrecheckRequest request = precheckRequestBuilder().authorization(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.PRECHECK));
    }

    private PrecheckRequest.PrecheckRequestBuilder<?, ?> precheckRequestBuilder() {
        return PrecheckRequest.builder()
                .uuid("uuid-tradicional-cashout-precheck")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode("100708854")
                .authorization("664071071637")
                .document("0901111112")
                .tipoDocumento(2);
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("TRADICIONAL CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }
}
