package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.EcuabetDepositReversePort;
import com.omnistack.backend.application.port.out.strategy.ReverseStrategy;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetDepositCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de REVERSE CASH_IN para reversar depositos ECUABET.
 */
@Component
@RequiredArgsConstructor
public class EcuabetDepositReverseStrategy implements ReverseStrategy {

    private static final String PROVIDER_KEY = "ecuabet";

    private final EcuabetDepositReversePort ecuabetDepositReversePort;
    private final AppProperties appProperties;

    /**
     * Indica si la estrategia atiende la capacidad y servicio configurados.
     *
     * @param serviceDefinition definicion del servicio seleccionada desde catalogo
     * @param capability capacidad solicitada
     * @return true cuando corresponde al REVERSE CASH_IN de ECUABET
     */
    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties();
        return capability == Capability.REVERSE
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && hasConfiguredOperation(provider, capability, serviceDefinition);
    }

    /**
     * Procesa el reverso de deposito ECUABET.
     *
     * @param request request interno recibido por OMNISTACK
     * @param serviceDefinition definicion catalogada del servicio
     * @param capability capacidad solicitada
     * @return respuesta interna mapeada para OMNISTACK
     */
    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        validateRequest(request, serviceDefinition, provider);
        AppProperties.ProviderOperationProperties operation = getRequiredOperation(provider, capability, serviceDefinition);
        Integer transactionId = parseTransactionId(request.getAuthorization());

        EcuabetDepositCommand command = EcuabetDepositCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .userid(request.getUserid())
                .phone(request.getPhone())
                .document(request.getDocument())
                .amount(request.getAmount())
                .transactionId(transactionId)
                .build();

        ExternalTransactionResponse externalResponse = ecuabetDepositReversePort.reverseDeposit(command, operation.getPath());
        return buildResponse(request, externalResponse, transactionId);
    }

    private ReverseResponse buildResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Integer transactionId) {
        Map<String, Object> payload = externalResponse.getPayload();
        Integer providerError = integerValue(payload, "error");
        boolean isError = providerError != null && providerError != 0;

        ReverseResponse.ReverseResponseBuilder<?, ?> builder = ReverseResponse.builder()
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
                .errorFlag(isError)
                .username(stringValue(payload, "name"))
                .lastname(stringValue(payload, "lastname"))
                .currency(stringValue(payload, "currency"))
                .authorization(String.valueOf(transactionId))
                .document(request.getDocument())
                .amount(resolveAmount(payload, request));

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(externalResponse.getExternalCode())
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(externalResponse.getExternalCode(), "Transaccion correcta"));
        }

        return builder.build();
    }

    private void validateRequest(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("category_code", request.getCategoryCode(), provider.getCategoryCode());
        validateValue("subcategory_code", request.getSubcategoryCode(), provider.getSubcategoryCode());
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode());
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode());
        if (request.getAmount() == null) {
            throw new IntegrationException("ECUABET requiere amount para reverso de deposito");
        }
        if (request.getAuthorization() == null || request.getAuthorization().isBlank()) {
            throw new IntegrationException("ECUABET requiere authorization para reverso de deposito");
        }
    }

    private void validateValue(String fieldName, String currentValue, String expectedValue) {
        if (expectedValue == null || expectedValue.isBlank()) {
            throw new IntegrationException("La configuracion de ECUABET no define el valor requerido para " + fieldName);
        }
        if (!expectedValue.equalsIgnoreCase(currentValue)) {
            throw new IntegrationException("La solicitud no coincide con la configuracion esperada de ECUABET para " + fieldName);
        }
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = findProviderProperties();
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor ECUABET");
        }
        return provider;
    }

    private AppProperties.ProviderProperties findProviderProperties() {
        return appProperties.getIntegration().getProviders().get(PROVIDER_KEY);
    }

    private boolean hasConfiguredOperation(
            AppProperties.ProviderProperties provider,
            Capability capability,
            ServiceDefinition serviceDefinition) {
        AppProperties.ProviderOperationProperties operation = findOperation(provider, capability, serviceDefinition.getMovementType());
        return operation != null
                && operation.getPath() != null
                && !operation.getPath().isBlank()
                && operation.getItem() != null
                && operation.getItem().equalsIgnoreCase(serviceDefinition.getRmsItemCode());
    }

    private AppProperties.ProviderOperationProperties getRequiredOperation(
            AppProperties.ProviderProperties provider,
            Capability capability,
            ServiceDefinition serviceDefinition) {
        AppProperties.ProviderOperationProperties operation = findOperation(provider, capability, serviceDefinition.getMovementType());
        if (operation == null || operation.getPath() == null || operation.getPath().isBlank()) {
            throw new IntegrationException("ECUABET no tiene ruta configurada para capability=" + capability.name()
                    + " y movement_type=" + serviceDefinition.getMovementType());
        }
        if (operation.getItem() == null || !operation.getItem().equalsIgnoreCase(serviceDefinition.getRmsItemCode())) {
            throw new IntegrationException("ECUABET no tiene item configurado para rms_item_code=" + serviceDefinition.getRmsItemCode()
                    + ", capability=" + capability.name()
                    + " y movement_type=" + serviceDefinition.getMovementType());
        }
        return operation;
    }

    private AppProperties.ProviderOperationProperties findOperation(
            AppProperties.ProviderProperties provider,
            Capability capability,
            MovementType movementType) {
        if (provider.getServices() == null || movementType == null) {
            return null;
        }
        AppProperties.ProviderCapabilityProperties capabilityProperties = provider.getServices().get(capability.name());
        if (capabilityProperties == null) {
            return null;
        }
        return movementType == MovementType.CASH_IN ? capabilityProperties.getCashin() : capabilityProperties.getCashout();
    }

    private Integer parseTransactionId(String authorization) {
        try {
            return Integer.valueOf(authorization);
        } catch (NumberFormatException exception) {
            throw new IntegrationException("ECUABET requiere authorization numerico para reverso de deposito");
        }
    }

    private String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal resolveAmount(Map<String, Object> payload, BaseTransactionRequest request) {
        String value = stringValue(payload, "amount");
        return value == null || value.isBlank() ? request.getAmount() : new BigDecimal(value);
    }

    private Integer integerValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
