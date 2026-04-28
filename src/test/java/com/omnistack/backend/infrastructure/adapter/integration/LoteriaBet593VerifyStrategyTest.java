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
import com.omnistack.backend.application.port.out.Bet593RechargeValidationPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593RechargeCommand;
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
class LoteriaBet593VerifyStrategyTest {

    @Mock
    private Bet593RechargeValidationPort bet593RechargeValidationPort;

    private LoteriaBet593VerifyStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setCategoryCode("1");
        provider.setSubcategoryCode("1");
        provider.setServiceProviderCode("2");

        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("10001565828");
        capabilityProperties.getCashin().setPath("/APIVentasLoteria/api/Ventas/ValidarBet593");
        capabilityProperties.getCashin().setCapabilities("VALIDA593");
        capabilityProperties.getCashin().setName("VALIDA593");
        provider.getServices().put("VERIFY", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(Map.of("loteria", provider)));

        strategy = new LoteriaBet593VerifyStrategy(bet593RechargeValidationPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredBet593CashinVerify() {
        ServiceDefinition bet593Service = serviceDefinition(MovementType.CASH_IN, "10001565828");
        ServiceDefinition cashoutService = serviceDefinition(MovementType.CASH_OUT, "10001565829");

        assertTrue(strategy.supports(bet593Service, Capability.VERIFY));
        assertFalse(strategy.supports(bet593Service, Capability.EXECUTE));
        assertFalse(strategy.supports(cashoutService, Capability.VERIFY));
    }

    @Test
    void shouldBuildValidationCommandAndReturnCommitMessage() {
        when(bet593RechargeValidationPort.validateRecharge(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("0")
                .externalMessage("")
                .payload(Map.of(
                        "error", 0,
                        "message", "",
                        "document", "0901111112",
                        "status", "COMMIT"))
                .build());

        VerifyRequest request = verifyRequestBuilder().build();

        VerifyResponse response = (VerifyResponse) strategy.process(
                request,
                serviceDefinition(MovementType.CASH_IN, "10001565828"),
                Capability.VERIFY);

        ArgumentCaptor<Bet593RechargeCommand> captor = ArgumentCaptor.forClass(Bet593RechargeCommand.class);
        verify(bet593RechargeValidationPort).validateRecharge(captor.capture(), org.mockito.ArgumentMatchers.eq(
                "/APIVentasLoteria/api/Ventas/ValidarBet593"));

        assertEquals("uuid-bet593", captor.getValue().getUuid());
        assertEquals("9F968187-F436-4F19-8C1F-A7A4DA07A899", captor.getValue().getAuthorization());
        assertEquals("7366ea56284a06a2", captor.getValue().getSerialnumber());
        assertEquals("0901111112", captor.getValue().getDocument());
        assertEquals("0", response.getStatus().getCode());
        assertEquals("Transaccion ha sido ejecutada", response.getStatus().getMessage());
        assertEquals("0901111112", response.getDocument());
    }

    @Test
    void shouldFailWhenValidationFieldsAreMissing() {
        VerifyRequest request = verifyRequestBuilder()
                .serialnumber(null)
                .build();

        assertThrows(IntegrationException.class,
                () -> strategy.process(request, serviceDefinition(MovementType.CASH_IN, "10001565828"), Capability.VERIFY));
    }

    private VerifyRequest.VerifyRequestBuilder<?, ?> verifyRequestBuilder() {
        return VerifyRequest.builder()
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
                .rmsItemCode("10001565828")
                .authorization("9F968187-F436-4F19-8C1F-A7A4DA07A899")
                .serialnumber("7366ea56284a06a2")
                .document("0901111112");
    }

    private ServiceDefinition serviceDefinition(MovementType movementType, String rmsItemCode) {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode(rmsItemCode)
                .description("BET 593 CASH IN")
                .movementType(movementType)
                .capabilities(List.of(Capability.VERIFY))
                .build();
    }
}
