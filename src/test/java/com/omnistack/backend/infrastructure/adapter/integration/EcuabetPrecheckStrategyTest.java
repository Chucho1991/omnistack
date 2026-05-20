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
import com.omnistack.backend.application.port.out.EcuabetUserSearchPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
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
class EcuabetPrecheckStrategyTest {

    @Mock
    private EcuabetUserSearchPort ecuabetUserSearchPort;

    private EcuabetPrecheckStrategy strategy;

    @BeforeEach
    void setUp() {
        AppProperties.ProviderProperties provider = new AppProperties.ProviderProperties();
        provider.setServiceProviderCode("1");
        AppProperties.ProviderCapabilityProperties capabilityProperties = new AppProperties.ProviderCapabilityProperties();
        capabilityProperties.getCashin().setItem("100713841");
        capabilityProperties.getCashin().setPath("/user/search");
        capabilityProperties.getCashin().setCapabilities("BUSCAR_USUARIO");
        capabilityProperties.getCashin().setName("BUSCAR_USUARIO");
        capabilityProperties.getCashout().setItem("100708846");
        capabilityProperties.getCashout().setPath("/user/searchwithdraw");
        capabilityProperties.getCashout().setCapabilities("BUSCAR_NOTA_RETIRO");
        capabilityProperties.getCashout().setName("BUSCAR_NOTA_RETIRO");
        provider.getServices().put("PRECHECK", capabilityProperties);

        AppProperties appProperties = new AppProperties();
        appProperties.getIntegration().setProviders(new HashMap<>(java.util.Map.of("ecuabet", provider)));

        strategy = new EcuabetPrecheckStrategy(ecuabetUserSearchPort, appProperties);
    }

    @Test
    void shouldSupportConfiguredEcuabetPrecheck() {
        ServiceDefinition ecuabetService = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .movementType(MovementType.CASH_IN)
                .build();
        ServiceDefinition otherProviderService = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("100708850")
                .movementType(MovementType.CASH_IN)
                .build();

        assertTrue(strategy.supports(ecuabetService, Capability.PRECHECK));
        assertFalse(strategy.supports(ecuabetService, Capability.EXECUTE));
        assertFalse(strategy.supports(otherProviderService, Capability.PRECHECK));
    }

    @Test
    void shouldBuildCommandAndReturnInternalResponse() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("00")
                .externalMessage("Usuario encontrado")
                .payload(java.util.Map.of(
                        "error", 0,
                        "name", "Carlos",
                        "userid", "997561"))
                .build());

        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .userid("997561")
                .phone("123456")
                .document("0912345678")
                .amount(new BigDecimal("12.50"))
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .description("ECUABET CASH IN")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();

        var response = strategy.process(request, serviceDefinition, Capability.PRECHECK);

        ArgumentCaptor<com.omnistack.backend.domain.model.EcuabetUserSearchCommand> captor =
                ArgumentCaptor.forClass(com.omnistack.backend.domain.model.EcuabetUserSearchCommand.class);
        verify(ecuabetUserSearchPort).searchUser(captor.capture(), org.mockito.ArgumentMatchers.eq("/user/search"));

        assertEquals("997561", captor.getValue().getUserid());
        assertEquals("123456", captor.getValue().getPhone());
        assertEquals("0912345678", captor.getValue().getDocument());
        assertEquals(new BigDecimal("12.50"), captor.getValue().getAmount());
        assertEquals("uuid-1", response.getUuid());
        assertEquals("00", response.getStatus().getCode());
        assertEquals("Carlos", ((com.omnistack.backend.application.dto.PrecheckResponse) response).getUsername());
    }

    @Test
    void shouldReturnErrorWhenExternalPrecheckIsRejected() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(false)
                .externalCode("101")
                .externalMessage("Usuario invalido")
                .payload(java.util.Map.of(
                        "code", "101",
                        "message", "Usuario invalido"))
                .build());

        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-rejected")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .document("2912912912")
                .amount(new BigDecimal("0.0010"))
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .description("ECUABET CASH IN")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();

        var response = (com.omnistack.backend.application.dto.PrecheckResponse)
                strategy.process(request, serviceDefinition, Capability.PRECHECK);

        assertTrue(response.isErrorFlag());
        assertEquals("02", response.getError().getCode());
        assertEquals("Usuario invalido", response.getError().getMessage());
        assertEquals(null, response.getAuthorization());
    }

    @Test
    void shouldPreserveEcuabetBusinessErrorCodeAndMessageInCanonicalResponse() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", "101");
        payload.put("error", 1);
        payload.put("message", "Usuario no encontrado");

        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(false)
                .externalCode("02")
                .externalMessage("Usuario no encontrado")
                .payload(payload)
                .build());

        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-rejected")
                .chain("60")
                .store("4")
                .storeName("Local 4")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .document("2912912912")
                .build();

        var response = (com.omnistack.backend.application.dto.PrecheckResponse)
                strategy.process(request, cashinServiceDefinition(), Capability.PRECHECK);

        assertTrue(response.isErrorFlag());
        assertEquals("02", response.getError().getCode());
        assertEquals("Usuario no encontrado", response.getError().getMessage());
        assertEquals(null, response.getAuthorization());
    }

    @Test
    void shouldUseCashoutOperationForWithdrawalPrecheck() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("00")
                .externalMessage("Nota encontrada")
                .payload(java.util.Map.of(
                        "error", 0,
                        "name", "USU FEMSA",
                        "currency", "USD",
                        "amount", "10",
                        "userid", "998765"))
                .build());

        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-cashout")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .withdrawId("7667")
                .password("88422")
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .description("ECUABET CASH OUT")
                .movementType(MovementType.CASH_OUT)
                .capabilities(List.of(Capability.PRECHECK))
                .build();

        var response = strategy.process(request, serviceDefinition, Capability.PRECHECK);

        ArgumentCaptor<com.omnistack.backend.domain.model.EcuabetUserSearchCommand> captor =
                ArgumentCaptor.forClass(com.omnistack.backend.domain.model.EcuabetUserSearchCommand.class);
        verify(ecuabetUserSearchPort).searchUser(captor.capture(), org.mockito.ArgumentMatchers.eq("/user/searchwithdraw"));

        assertEquals(MovementType.CASH_OUT, captor.getValue().getMovementType());
        assertEquals("7667", captor.getValue().getWithdrawId());
        assertEquals("88422", captor.getValue().getPassword());
        assertEquals("998765", ((com.omnistack.backend.application.dto.PrecheckResponse) response).getUserid());
        assertEquals(new BigDecimal("10"), ((com.omnistack.backend.application.dto.PrecheckResponse) response).getAmount());
    }

    @Test
    void shouldRejectCashoutPrecheckWhenRequestedAmountIsGreaterThanProviderAmount() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("00")
                .externalMessage("Nota encontrada")
                .payload(java.util.Map.of(
                        "error", 0,
                        "name", "USU FEMSA",
                        "currency", "USD",
                        "amount", "10.00",
                        "userid", "998765"))
                .build());

        var response = (com.omnistack.backend.application.dto.PrecheckResponse) strategy.process(
                cashoutRequest(new BigDecimal("12.50")),
                cashoutServiceDefinition(),
                Capability.PRECHECK);

        assertTrue(response.isErrorFlag());
        assertEquals("01", response.getError().getCode());
        assertEquals("El monto solicitado 12.50 es mayor que el monto retornado por ECUABET 10.00",
                response.getError().getMessage());
        assertEquals(new BigDecimal("10.00"), response.getAmount());
    }

    @Test
    void shouldRejectCashoutPrecheckWhenRequestedAmountIsLessThanProviderAmount() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("00")
                .externalMessage("Nota encontrada")
                .payload(java.util.Map.of(
                        "error", 0,
                        "name", "USU FEMSA",
                        "currency", "USD",
                        "amount", "10.00",
                        "userid", "998765"))
                .build());

        var response = (com.omnistack.backend.application.dto.PrecheckResponse) strategy.process(
                cashoutRequest(new BigDecimal("8.00")),
                cashoutServiceDefinition(),
                Capability.PRECHECK);

        assertTrue(response.isErrorFlag());
        assertEquals("01", response.getError().getCode());
        assertEquals("El monto solicitado 8.00 es menor que el monto retornado por ECUABET 10.00",
                response.getError().getMessage());
    }

    @Test
    void shouldApproveCashoutPrecheckWhenRequestedAmountMatchesProviderAmountWithDifferentScale() {
        when(ecuabetUserSearchPort.searchUser(any(), anyString())).thenReturn(ExternalTransactionResponse.builder()
                .approved(true)
                .externalCode("00")
                .externalMessage("Nota encontrada")
                .payload(java.util.Map.of(
                        "error", 0,
                        "name", "USU FEMSA",
                        "currency", "USD",
                        "amount", "10",
                        "userid", "998765"))
                .build());

        var response = (com.omnistack.backend.application.dto.PrecheckResponse) strategy.process(
                cashoutRequest(new BigDecimal("10.00")),
                cashoutServiceDefinition(),
                Capability.PRECHECK);

        assertFalse(response.isErrorFlag());
        assertEquals(new BigDecimal("10"), response.getAmount());
    }

    @Test
    void shouldFailWhenProviderCodeDoesNotMatch() {
        PrecheckRequest request = PrecheckRequest.builder()
                .uuid("uuid-1")
                .chain("1")
                .store("148")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_IN)
                .categoryCode("5")
                .subcategoryCode("1")
                .serviceProviderCode("9")
                .rmsItemCode("100713841")
                .userid("997561")
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("5")
                .subcategoryCode("1")
                .serviceProviderCode("9")
                .rmsItemCode("100713841")
                .description("Servicio incorrecto")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();

        assertThrows(IntegrationException.class, () -> strategy.process(request, serviceDefinition, Capability.PRECHECK));
    }

    private PrecheckRequest cashoutRequest(BigDecimal amount) {
        return PrecheckRequest.builder()
                .uuid("uuid-cashout")
                .chain("1")
                .store("148")
                .storeName("FYBECA AMAZONAS")
                .pos("1")
                .channelPos(ChannelPos.POS)
                .movementType(MovementType.CASH_OUT)
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .withdrawId("7667")
                .password("88422")
                .amount(amount)
                .build();
    }

    private ServiceDefinition cashinServiceDefinition() {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100713841")
                .description("ECUABET CASH IN")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }

    private ServiceDefinition cashoutServiceDefinition() {
        return ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("100708846")
                .description("ECUABET CASH OUT")
                .movementType(MovementType.CASH_OUT)
                .capabilities(List.of(Capability.PRECHECK))
                .build();
    }
}
