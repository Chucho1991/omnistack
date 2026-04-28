package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.Bet593WithdrawPort;
import com.omnistack.backend.application.port.out.strategy.ExecuteStrategy;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Estrategia de EXECUTE CASH_OUT para nota de retiro BET593 mediante Loteria Nacional.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoteriaBet593WithdrawExecuteStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "loteria";

    private final Bet593WithdrawPort bet593WithdrawPort;
    private final AppProperties appProperties;

    /**
     * Indica si la estrategia soporta el servicio y capacidad resueltos.
     *
     * @param serviceDefinition definicion comercial resuelta desde catalogo
     * @param capability capacidad transaccional solicitada
     * @return true cuando corresponde al EXECUTE CASH_OUT BET593
     */
    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties();
        return capability == Capability.EXECUTE
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_OUT
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && hasConfiguredOperation(provider, capability, serviceDefinition);
    }

    /**
     * Procesa la nota de retiro BET593 y delega el consumo externo al puerto configurado.
     *
     * @param request request canonico interno
     * @param serviceDefinition definicion comercial resuelta
     * @param capability capacidad transaccional solicitada
     * @return response canonico de execute
     */
    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties();
        validateBusinessContext(request, serviceDefinition, provider);
        validateRequiredRequestFields(request);
        AppProperties.ProviderOperationProperties operation = getRequiredOperation(provider, capability, serviceDefinition);

        Bet593WithdrawCommand command = Bet593WithdrawCommand.builder()
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
                .userid(request.getUserid())
                .phone(request.getPhone())
                .withdrawId(request.getWithdrawId())
                .password(request.getPassword())
                .authorization(request.getAuthorization())
                .serialnumber(request.getSerialnumber())
                .document(request.getDocument())
                .amount(request.getAmount())
                .build();

        ExternalTransactionResponse externalResponse = bet593WithdrawPort.withdraw(command, operation.getPath());
        return buildResponse(request, externalResponse);
    }

    private ExecuteResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean isError = stringValue(payload, "message") != null && !stringValue(payload, "message").isBlank();

        ExecuteResponse.ExecuteResponseBuilder<?, ?> builder = ExecuteResponse.builder()
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
                .authorization(resolveValue(payload, "authorization", request.getAuthorization()))
                .serialnumber(resolveValue(payload, "serialnumber", request.getSerialnumber()))
                .userid(resolveValue(payload, "userid", request.getUserid()))
                .document(resolveValue(payload, "document", request.getDocument()))
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

    private void validateBusinessContext(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("category_code", request.getCategoryCode(), provider.getCategoryCode());
        validateValue("subcategory_code", request.getSubcategoryCode(), provider.getSubcategoryCode());
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode());
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode());
        validateValue("subcategory_code", serviceDefinition.getSubcategoryCode(), provider.getSubcategoryCode());
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode());
    }

    private void validateRequiredRequestFields(BaseTransactionRequest request) {
        if (request.getDocument() == null || request.getDocument().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere document para nota de retiro");
        }
        if (request.getWithdrawId() == null || request.getWithdrawId().isBlank()) {
            throw new IntegrationException("Loteria BET593 requiere withdrawId para nota de retiro");
        }
    }

    private void validateValue(String fieldName, String currentValue, String expectedValue) {
        if (expectedValue == null || expectedValue.isBlank()) {
            throw new IntegrationException("La configuracion de Loteria BET593 no define el valor requerido para " + fieldName);
        }
        if (!expectedValue.equalsIgnoreCase(currentValue)) {
            throw new IntegrationException("La solicitud no coincide con la configuracion esperada de Loteria BET593 para " + fieldName);
        }
    }

    private AppProperties.ProviderProperties getProviderProperties() {
        AppProperties.ProviderProperties provider = findProviderProperties();
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor Loteria");
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
            throw new IntegrationException("Loteria BET593 no tiene ruta configurada para capability=" + capability.name()
                    + " y movement_type=" + serviceDefinition.getMovementType());
        }
        if (operation.getItem() == null || !operation.getItem().equalsIgnoreCase(serviceDefinition.getRmsItemCode())) {
            throw new IntegrationException("Loteria BET593 no tiene item configurado para rms_item_code="
                    + serviceDefinition.getRmsItemCode());
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

    private String resolveValue(Map<String, Object> payload, String key, String fallback) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? fallback : value;
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
}
