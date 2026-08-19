package com.omnistack.backend.infrastructure.adapter.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.CreateTicketResponse;
import com.omnistack.backend.application.port.out.TradicionalNumerosQueryPort;
import com.omnistack.backend.application.port.out.TradicionalSorteosQueryPort;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.TradicionalNumerosQueryCommand;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradicionalCreateTicketStrategyTest {

    private static final String WS_KEY = "CREATE_TICKET.CASHIN";
    private static final String RMS_ITEM_CODE = "100713842";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/RecuperarNumerosDisponiblesPorCombinacion";

    @Mock
    private TradicionalSorteosQueryPort sorteosQueryPort;
    @Mock
    private TradicionalNumerosQueryPort numerosQueryPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private TradicionalCreateTicketStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setServiceProviderCode("3445");
        provider.setMedioId(23);
        provider.getAuth().getLogin().setUsername("usrtiaprep");

        when(providerConfigService.getProviderProperties("tradicional")).thenReturn(provider);
        when(providerWsDefsService.hasItem("tradicional", WS_KEY, RMS_ITEM_CODE)).thenReturn(true);
        when(providerWsService.hasUrl("tradicional", WS_KEY)).thenReturn(true);
        when(providerWsService.requireUrl(eq("tradicional"), eq(WS_KEY), any())).thenReturn(OPERATION_URL);
        when(providerWsDefsService.getString("tradicional", WS_KEY, "juego_id." + RMS_ITEM_CODE)).thenReturn("1");
        when(providerWsService.findUrl(eq("tradicional"), eq("PRECHECK_SORTEOS.CASHIN"))).thenReturn(Optional.empty());
        // Sin filas en WS_DEFS para 'sugerir'/'registros' -> simula que no estan parametrizados
        when(providerWsDefsService.getString("tradicional", WS_KEY, "sugerir")).thenReturn(null);
        when(providerWsDefsService.getInteger("tradicional", WS_KEY, "registros")).thenReturn(null);
        when(numerosQueryPort.queryNumeros(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of("listaNumeros", List.of(), "numeroReserva", "RES-1"))
                .build());

        strategy = new TradicionalCreateTicketStrategy(
                sorteosQueryPort, numerosQueryPort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredTradicionalCashInCreateTicket() {
        ServiceDefinition cashInService = serviceDefinition(MovementType.CASH_IN);
        ServiceDefinition cashOutService = ServiceDefinition.builder()
                .serviceProviderCode("3445").rmsItemCode(RMS_ITEM_CODE)
                .movementType(MovementType.CASH_OUT).capabilities(List.of(Capability.CREATE_TICKET)).build();

        assertTrue(strategy.supports(cashInService, Capability.CREATE_TICKET));
        assertFalse(strategy.supports(cashInService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashOutService, Capability.CREATE_TICKET));
    }

    @Test
    void shouldThrowWhenDrawIdIsMissing() {
        CreateTicketRequest request = requestBuilder().drawId(null).build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN), Capability.CREATE_TICKET));
    }

    @Test
    void shouldDefaultSugerirTrueAndRegistrosTenWhenNotParametrized() {
        // WS_DEFS sin filas para 'sugerir'/'registros' -> getString/getInteger devuelven null
        CreateTicketRequest request = requestBuilder().build();

        CreateTicketResponse response = (CreateTicketResponse) strategy.process(
                request, serviceDefinition(MovementType.CASH_IN), Capability.CREATE_TICKET);

        ArgumentCaptor<TradicionalNumerosQueryCommand> captor = ArgumentCaptor.forClass(TradicionalNumerosQueryCommand.class);
        verify(numerosQueryPort).queryNumeros(captor.capture(), eq(OPERATION_URL));
        assertTrue(captor.getValue().getSugerir());
        assertEquals(10, captor.getValue().getRegistros());
        assertFalse(response.isErrorFlag());
        assertEquals("RES-1", response.getReservaId());
    }

    @Test
    void shouldUseSugerirAndRegistrosParametrizedInWsDefs() {
        when(providerWsDefsService.getString("tradicional", WS_KEY, "sugerir")).thenReturn("false");
        when(providerWsDefsService.getInteger("tradicional", WS_KEY, "registros")).thenReturn(5);

        CreateTicketRequest request = requestBuilder().build();
        strategy.process(request, serviceDefinition(MovementType.CASH_IN), Capability.CREATE_TICKET);

        ArgumentCaptor<TradicionalNumerosQueryCommand> captor = ArgumentCaptor.forClass(TradicionalNumerosQueryCommand.class);
        verify(numerosQueryPort).queryNumeros(captor.capture(), eq(OPERATION_URL));
        assertFalse(captor.getValue().getSugerir());
        assertEquals(5, captor.getValue().getRegistros());
    }

    private CreateTicketRequest.CreateTicketRequestBuilder<?, ?> requestBuilder() {
        return CreateTicketRequest.builder()
                .uuid("uuid-tradicional-create-ticket")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("984")
                .subcategoryCode("1122")
                .serviceProviderCode("3445")
                .rmsItemCode(RMS_ITEM_CODE)
                .drawId("7151")
                .combinacion("");
    }

    private ServiceDefinition serviceDefinition(MovementType movementType) {
        return ServiceDefinition.builder()
                .categoryCode("984")
                .subcategoryCode("1122")
                .serviceProviderCode("3445")
                .rmsItemCode(RMS_ITEM_CODE)
                .description("TRADICIONAL CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.CREATE_TICKET))
                .build();
    }
}
