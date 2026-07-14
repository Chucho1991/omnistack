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
import com.omnistack.backend.application.port.out.TradicionalPagoPremioPort;
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
class LoteriaTradicionalCashOutExecuteStrategyTest {

    private static final String WS_KEY = "EXECUTE.CASHOUT";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/PagoPremioTicketTradicional";

    @Mock
    private TradicionalPagoPremioPort pagoPremioPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private LoteriaTradicionalCashOutExecuteStrategy strategy;

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

        strategy = new LoteriaTradicionalCashOutExecuteStrategy(
                pagoPremioPort, providerConfigService, providerWsDefsService, providerWsService);
    }

    @Test
    void shouldSupportConfiguredTradicionalCashOutExecute() {
        ServiceDefinition cashOutService = serviceDefinition(MovementType.CASH_OUT, "100708854");
        ServiceDefinition cashInService = serviceDefinition(MovementType.CASH_IN, "100713842");

        assertTrue(strategy.supports(cashOutService, Capability.EXECUTE));
        assertFalse(strategy.supports(cashOutService, Capability.PRECHECK));
        assertFalse(strategy.supports(cashInService, Capability.EXECUTE));
    }

    @Test
    void shouldPayPremioAndReturnExecuteResponse() {
        when(pagoPremioPort.pagoPremio(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "mpi", "957",
                        "pci", "999",
                        "ref", "473856",
                        "authorization", "999"))
                .build());

        ExecuteResponse response = (ExecuteResponse) strategy.process(
                executeRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.EXECUTE);

        assertFalse(response.isErrorFlag());
        assertEquals("999", response.getAuthorization());
        assertEquals("999", response.getPci());
        assertEquals("473856", response.getRef());
        assertEquals("1000000.00", response.getAmount().toPlainString());
    }

    @Test
    void shouldFailWhenMpiIsMissing() {
        ExecuteRequest request = executeRequestBuilder().mpi(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.EXECUTE));
    }

    @Test
    void shouldFailWhenAuthorizationIsMissing() {
        ExecuteRequest request = executeRequestBuilder().authorization(null).build();

        assertThrows(IntegrationException.class, () -> strategy.process(
                request,
                serviceDefinition(MovementType.CASH_OUT, "100708854"),
                Capability.EXECUTE));
    }

    private ExecuteRequest.ExecuteRequestBuilder<?, ?> executeRequestBuilder() {
        return ExecuteRequest.builder()
                .uuid("uuid-tradicional-cashout-execute")
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
                .username("Ariel Castillo")
                .tipoDocumento(2)
                .mpi("957")
                .amount(new BigDecimal("1000000.00"));
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("2")
                .subcategoryCode("2")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("TRADICIONAL CASH OUT")
                .movementType(movementType)
                .capabilities(List.of(Capability.EXECUTE))
                .build();
    }
}
