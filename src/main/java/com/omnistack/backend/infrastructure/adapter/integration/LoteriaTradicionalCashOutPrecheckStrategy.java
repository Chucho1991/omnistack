package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.TradicionalConsultarTicketPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.domain.model.TradicionalConsultarTicketCommand;
import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.exception.BusinessException;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de PRECHECK CASH_OUT para Tradicionales (Loteria/Lotto/Pozo). Llama a ConsultarTicket
 * para validar que el boleto tiene premio y aun no ha sido pagado, antes de permitir el pago.
 */
@Component
@RequiredArgsConstructor
public class LoteriaTradicionalCashOutPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String PROVIDER_NAME = "Loteria Tradicionales";

    private final TradicionalConsultarTicketPort consultarTicketPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.PRECHECK
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_OUT
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && hasConfiguredOperation(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition);
    }

    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties(providerConfigService, PROVIDER_KEY, PROVIDER_NAME);

        if (request.getAuthorization() == null || request.getAuthorization().isBlank()) {
            throw new IntegrationException("Tradicionales requiere authorization (clave del boleto electronico) para PRECHECK de CASH_OUT");
        }

        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);
        String clienteId = provider.getClienteId() != null
                ? String.valueOf(provider.getClienteId())
                : (provider.getShopId() != null ? provider.getShopId() : "");

        Integer tipoDocumento = null;
        String nombreGanador = null;
        if (request instanceof PrecheckRequest precheckRequest) {
            tipoDocumento = precheckRequest.getTipoDocumento();
            nombreGanador = precheckRequest.getNombreGanador();
        }

        TradicionalConsultarTicketCommand command = TradicionalConsultarTicketCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .userId(provider.getAuth().getLogin().getUsername())
                .clienteId(clienteId)
                .mor(request.getUuid())
                .tipoDocumento(tipoDocumento)
                .numeroDocumento(request.getDocument())
                .nombreGanador(nombreGanador)
                .cl(request.getAuthorization())
                .premioEspecie(Boolean.FALSE)
                .build();

        ExternalTransactionResponse externalResponse = consultarTicketPort.consultarTicket(command, operationUrl);
        if (!externalResponse.isApproved()) {
            throw new BusinessException(externalResponse.getExternalMessage());
        }

        Map<String, Object> payload = externalResponse.getPayload();
        Boolean isWinner = getBooleanValue(payload, "is_winner");
        if (isWinner == null || !isWinner) {
            throw new BusinessException("El boleto no tiene premio para cobrar: " + stringValue(payload, "ticket_status"));
        }

        return buildResponse(request, payload);
    }

    private PrecheckResponse buildResponse(BaseTransactionRequest request, Map<String, Object> payload) {
        BigDecimal prizeAmount = decimalValue(payload, "prize_amount");
        return PrecheckResponse.builder()
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .errorFlag(false)
                .authorization(stringValue(payload, "authorization"))
                .amount(prizeAmount)
                .ticketStatus(stringValue(payload, "ticket_status"))
                .winner(Boolean.TRUE)
                .prizeAmount(prizeAmount)
                .mpi(stringValue(payload, "mpi"))
                .status(new StatusDetail(StatusCodes.SUCCESS, "Boleto con premio disponible para pago"))
                .build();
    }

    private Boolean getBooleanValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value != null) {
            return Boolean.valueOf(String.valueOf(value));
        }
        return null;
    }
}
