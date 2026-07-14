package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.port.out.TradicionalVentaBoletosPort;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.TradicionalVentaBoletosCommand;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia EXECUTE CASH_IN para LN Tradicionales. Llama a VentaBoletos.
 */
@Component
@RequiredArgsConstructor
public class LoteriaTradicionalExecuteStrategy extends AbstractProviderStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "tradicional";
    private static final String PROVIDER_NAME = "Loteria Tradicionales";
    private static final String JUEGO_ID_FIELD_PREFIX = "juego_id";

    private final TradicionalVentaBoletosPort ventaBoletosPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.EXECUTE
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
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
        validateBusinessContext(request, serviceDefinition, provider);

        if (!(request instanceof ExecuteRequest executeRequest)) {
            throw new IntegrationException("Tradicionales EXECUTE requiere un ExecuteRequest");
        }
        List<ExecuteRequest.BoletoData> boletosData = executeRequest.getListaBoletos() != null && !executeRequest.getListaBoletos().isEmpty()
                ? executeRequest.getListaBoletos()
                : (executeRequest.getBoletoData() != null ? List.of(executeRequest.getBoletoData()) : null);
        if (boletosData == null || boletosData.isEmpty()) {
            throw new IntegrationException("Tradicionales requiere boleto_data o lista_boletos para EXECUTE");
        }
        if (executeRequest.getAmount() == null) {
            throw new IntegrationException("Tradicionales requiere amount para EXECUTE");
        }
        if (executeRequest.getReservaId() == null || executeRequest.getReservaId().isBlank()) {
            throw new IntegrationException("Tradicionales requiere reserva_id (devuelto por CREATE_TICKET) para EXECUTE");
        }

        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

        String clienteId = provider.getClienteId() != null ? String.valueOf(provider.getClienteId())
                : (provider.getShopId() != null ? provider.getShopId() : "");

        TradicionalVentaBoletosCommand command = TradicionalVentaBoletosCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .medioId(provider.getMedioId())
                .userName(provider.getAuth().getLogin().getUsername())
                .cliente(clienteId)
                .ordenCompra(request.getUuid())
                .reservaId(executeRequest.getReservaId())
                .totalVenta(executeRequest.getAmount())
                .formaCobro(provider.getCanal() != null ? provider.getCanal() : "BMV")
                .numeroIdentificacion(request.getDocument())
                .nombreComprador(executeRequest.getUsername())
                .numeroCelularComprador(request.getPhone())
                .correoElectronicoComprador(null)
                .boletos(boletosData.stream()
                        .map(boletoData -> TradicionalVentaBoletosCommand.BoletoEntry.builder()
                                .juegoId(resolveItemDefault(
                                        boletoData.getGameId(), providerWsDefsService, PROVIDER_KEY,
                                        toWsKey(capability.name(), serviceDefinition.getMovementType()),
                                        JUEGO_ID_FIELD_PREFIX, request.getRmsItemCode(), PROVIDER_NAME))
                                .sorteoId(boletoData.getDrawId())
                                .numero(boletoData.getNumero())
                                .cantidadBoletos(boletoData.getCantidadBoletos() != null ? boletoData.getCantidadBoletos() : 1)
                                .build())
                        .collect(java.util.stream.Collectors.toList()))
                .build();

        ExternalTransactionResponse externalResponse = ventaBoletosPort.ventaBoletos(command, operationUrl);
        return buildResponse(request, externalResponse);
    }

    private ExecuteResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean isError = !externalResponse.isApproved();

        ExecuteResponse.ExecuteResponseBuilder<?, ?> builder = ExecuteResponse.builder()
                .chain(request.getChain()).store(request.getStore()).storeName(request.getStoreName())
                .pos(request.getPos()).channelPos(request.getChannelPos().name())
                .uuid(request.getUuid())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .errorFlag(isError)
                .authorization(stringValue(payload, "authorization"))
                .amount(request.getAmount())
                .boletoClave(stringValue(payload, "boletoClave"))
                .boletoQr(stringValue(payload, "boletoQr"))
                .fechaVenta(stringValue(payload, "fechaVenta"))
                .fraccionesVendidas(stringValue(payload, "fraccionesVendidas"));

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, "Venta boleto Tradicionales completada"));
        }

        return builder.build();
    }

    private void validateBusinessContext(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("category_code", request.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }
}
