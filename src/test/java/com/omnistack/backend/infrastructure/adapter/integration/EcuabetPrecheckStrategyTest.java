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
        capabilityProperties.getCashin().setItem("10001565826");
        capabilityProperties.getCashin().setPath("/user/search");
        capabilityProperties.getCashin().setCapabilities("BUSCAR_USUARIO");
        capabilityProperties.getCashin().setName("BUSCAR_USUARIO");
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
                .rmsItemCode("10001565826")
                .movementType(MovementType.CASH_IN)
                .build();
        ServiceDefinition otherProviderService = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("2")
                .rmsItemCode("10001565828")
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
                .rmsItemCode("10001565826")
                .userid("997561")
                .phone("123456")
                .document("0912345678")
                .amount(new BigDecimal("12.50"))
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("1")
                .subcategoryCode("1")
                .serviceProviderCode("1")
                .rmsItemCode("10001565826")
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
        assertEquals("00", response.getProviderCode());
        assertEquals("00", response.getStatus().getCode());
        assertEquals("Carlos", ((com.omnistack.backend.application.dto.PrecheckResponse) response).getUsername());
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
                .rmsItemCode("10001565826")
                .userid("997561")
                .build();

        ServiceDefinition serviceDefinition = ServiceDefinition.builder()
                .categoryCode("5")
                .subcategoryCode("1")
                .serviceProviderCode("9")
                .rmsItemCode("10001565826")
                .description("Servicio incorrecto")
                .movementType(MovementType.CASH_IN)
                .capabilities(List.of(Capability.PRECHECK))
                .build();

        assertThrows(IntegrationException.class, () -> strategy.process(request, serviceDefinition, Capability.PRECHECK));
    }
}
