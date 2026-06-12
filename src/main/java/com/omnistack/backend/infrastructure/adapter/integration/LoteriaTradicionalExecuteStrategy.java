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
                && serviceDefinition.getSubcategoryCode() != null
                && serviceDefinition.getSubcategoryCode().equalsIgnoreCase(provider.getSubcategoryCode())
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
        if (executeRequest.getBoletoData() == null) {
            throw new IntegrationException("Tradicionales requiere boleto_data para EXECUTE");
        }
        if (executeRequest.getAmount() == null) {
            throw new IntegrationException("Tradicionales requiere amount para EXECUTE");
        }

        ExecuteRequest.BoletoData boletoData = executeRequest.getBoletoData();
        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

        String clienteId = provider.getShopId() != null ? provider.getShopId()
                : (provider.getClienteId() != null ? String.valueOf(provider.getClienteId()) : "");

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
                .reservaId(request.getUuid())
                .totalVenta(executeRequest.getAmount())
                .formaCobro(provider.getCanal() != null ? provider.getCanal() : "BMV")
                .numeroIdentificacion(request.getDocument())
                .nombreComprador(executeRequest.getUsername())
                .numeroCelularComprador(request.getPhone())
                .correoElectronicoComprador(null)
                .juegoId(boletoData.getGameId())
                .sorteoId(boletoData.getDrawId())
                .numero(boletoData.getNumero())
                .cantidadBoletos(boletoData.getCantidadBoletos() != null ? boletoData.getCantidadBoletos() : 1)
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
                .fechaVenta(stringValue(payload, "fechaVenta"));

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
        validateValue("subcategory_code", request.getSubcategoryCode(), provider.getSubcategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("subcategory_code", serviceDefinition.getSubcategoryCode(), provider.getSubcategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }
}
