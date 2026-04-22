package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.mapper.ResponseFactory;
import com.omnistack.backend.application.port.out.EcuabetUserSearchPort;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de PRECHECK para la operacion Buscar usuario de ECUABET.
 */
@Component
@RequiredArgsConstructor
public class EcuabetPrecheckStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "ecuabet";

    private final EcuabetUserSearchPort ecuabetUserSearchPort;
    private final AppProperties appProperties;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties();
        return capability == Capability.PRECHECK
                && provider != null
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && hasConfiguredOperation(provider, capability, serviceDefinition);
    }

    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        validateProviderCode(request, serviceDefinition, provider);
        AppProperties.ProviderOperationProperties operation = getRequiredOperation(provider, capability, serviceDefinition);

        EcuabetUserSearchCommand command = EcuabetUserSearchCommand.builder()
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
                .withdrawId(request.getWithdrawId())
                .password(request.getPassword())
                .amount(request.getAmount())
                .build();

        ExternalTransactionResponse externalResponse = ecuabetUserSearchPort.searchUser(command, operation.getPath());
        return ResponseFactory.transactionResponse(request, externalResponse, capability);
    }

    private void validateProviderCode(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode());
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode());
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
}
