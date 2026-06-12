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
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
class LoteriaBet593WithdrawPrecheckStrategyTest {

    private static final String WS_KEY = "PRECHECK.CASHOUT";
    private static final String OPERATION_URL = "/APIVentasLoteria/api/Ventas/ConsultarRetiroBet593";

    @Mock
    private Bet593WithdrawValidationPort bet593WithdrawValidationPort;
    @Mock
    private ProviderConfigService providerConfigService;
    @Mock
    private ProviderWsDefsService providerWsDefsService;
    @Mock
    private ProviderWsService providerWsService;

    private LoteriaBet593WithdrawPrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        when(providerConfigService.getProviderProperties("loteria")).thenReturn(provider);
        when(providerWsDefsService.getString("loteria", WS_KEY, "item")).thenReturn("100708848");
        when(providerWsService.hasUrl("loteria", WS_KEY)).thenReturn(true);
        when(providerWsService.requireUrl(any(), any(), any())).thenReturn(OPERATION_URL);

        strategy = new LoteriaBet593WithdrawPrecheckStrategy(
                bet593WithdrawValidationPort, providerConfigService, providerWsDefsService, providerWsService);
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
        verify(bet593WithdrawValidationPort).validateWithdraw(captor.capture(), org.mockito.ArgumentMatchers.eq(OPERATION_URL));

        assertEquals("uuid-bet593-cashout-precheck", captor.getValue().getUuid());
        assertEquals("340468406359", captor.getValue().getWithdrawId());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("00", response.getStatus().getCode());
        assertEquals("Transaccion correcta", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
        assertEquals("17.00", response.getAmount().toPlainString());
    }

    @Test
    void shouldTreatExecutedWithdrawCodeAsNonError() {
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
    void shouldReturnErrorWhenAmountMismatches() {
        when(bet593WithdrawValidationPort.validateWithdraw(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of("amount", "20.00"))
                .build());

        PrecheckResponse response = (PrecheckResponse) strategy.process(
                precheckRequestBuilder().build(),
                serviceDefinition(MovementType.CASH_OUT, "100708848"),
                Capability.PRECHECK);

        assertTrue(response.isErrorFlag());
        assertEquals("01", response.getError().getCode());
    }

    @Test
    void shouldFailWhenWithdrawIdIsMissing() {
        PrecheckRequest request = precheckRequestBuilder()
                .withdrawId(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_OUT, "100708848"), Capability.PRECHECK));
    }

    @Test
    void shouldFailWhenDocumentIsMissing() {
        PrecheckRequest request = precheckRequestBuilder()
                .document(null)
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
