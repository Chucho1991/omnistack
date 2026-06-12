package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.mapper.ResponseFactory;
import com.omnistack.backend.application.port.out.EcuabetUserSearchPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.EcuabetUserSearchCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de PRECHECK para la operacion Buscar usuario de ECUABET.
 */
@Component
@RequiredArgsConstructor
public class EcuabetPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "ecuabet";
    private static final String PROVIDER_NAME = "ECUABET";

    private final EcuabetUserSearchPort ecuabetUserSearchPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.PRECHECK
                && provider != null
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
        validateProviderCode(request, serviceDefinition, provider);
        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

        EcuabetUserSearchCommand command = EcuabetUserSearchCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos())
                .movementType(serviceDefinition.getMovementType())
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

        ExternalTransactionResponse externalResponse = ecuabetUserSearchPort.searchUser(command, operationUrl);
        return ResponseFactory.transactionResponse(request, externalResponse, capability);
    }

    private ExternalTransactionResponse validateCashoutAmount(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            ExternalTransactionResponse externalResponse) {
        if (serviceDefinition.getMovementType() != MovementType.CASH_OUT
                || request.getAmount() == null
                || externalResponse == null
                || externalResponse.getPayload() == null
                || !externalResponse.isApproved()) {
            return externalResponse;
        }

        BigDecimal providerAmount = decimalValue(externalResponse.getPayload().get("amount"));
        if (providerAmount == null || request.getAmount().compareTo(providerAmount) == 0) {
            return externalResponse;
        }

        String direction = request.getAmount().compareTo(providerAmount) > 0 ? "mayor" : "menor";
        return ExternalTransactionResponse.builder()
                .approved(false)
                .externalCode(StatusCodes.VALIDATION_FAILED)
                .externalMessage("El monto solicitado " + request.getAmount()
                        + " es " + direction
                        + " que el monto retornado por ECUABET " + providerAmount)
                .payload(externalResponse.getPayload())
                .build();
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        String textValue = String.valueOf(value).trim();
        return textValue.isBlank() ? null : new BigDecimal(textValue);
    }

    private void validateProviderCode(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }
}
