package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.application.port.out.EcuabetWithdrawPort;
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
import com.omnistack.backend.domain.model.EcuabetWithdrawCommand;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de EXECUTE CASH_OUT para nota de retiro ECUABET.
 */
@Component
@RequiredArgsConstructor
public class EcuabetWithdrawExecuteStrategy extends AbstractProviderStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "ecuabet";
    private static final String PROVIDER_NAME = "ECUABET";
    private static final int MIN_TRANSACTION_ID = 10_000;
    private static final int MAX_TRANSACTION_ID = 999_999_999;

    private final EcuabetWithdrawPort ecuabetWithdrawPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.EXECUTE
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
        validateRequest(request);
        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);
        Integer transactionId = generateTransactionId();

        EcuabetWithdrawCommand command = EcuabetWithdrawCommand.builder()
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
                .withdrawId(request.getWithdrawId())
                .password(request.getPassword())
                .document(request.getDocument())
                .amount(request.getAmount())
                .transactionId(transactionId)
                .build();

        ExternalTransactionResponse externalResponse = ecuabetWithdrawPort.withdraw(command, operationUrl);
        return buildResponse(request, externalResponse, transactionId);
    }

    private ExecuteResponse buildResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Integer transactionId) {
        Map<String, Object> payload = externalResponse.getPayload();
        Integer providerError = integerValue(payload, "error");
        boolean isError = !externalResponse.isApproved() || providerError != null && providerError != 0;

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
                .document(request.getDocument())
                .amount(resolveAmount(payload, request));

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.authorization(resolveValue(payload, "authorization", String.valueOf(transactionId)))
                    .status(new StatusDetail(StatusCodes.SUCCESS, "Transaccion correcta"));
        }

        return builder.build();
    }

    private void validateRequest(BaseTransactionRequest request) {
        // category_code, subcategory_code y service_provider_code ya fueron validados
        // por el catalogo RMS al resolver el ServiceDefinition en DefaultProviderFlowResolver.
        if (request.getWithdrawId() == null || request.getWithdrawId().isBlank()) {
            throw new IntegrationException("ECUABET requiere withdrawId para nota de retiro");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IntegrationException("ECUABET requiere password para nota de retiro");
        }
        if (request.getAmount() == null) {
            throw new IntegrationException("ECUABET requiere amount para nota de retiro");
        }
    }

    private Integer generateTransactionId() {
        return ThreadLocalRandom.current().nextInt(MIN_TRANSACTION_ID, MAX_TRANSACTION_ID);
    }

    private java.math.BigDecimal resolveAmount(Map<String, Object> payload, BaseTransactionRequest request) {
        String value = stringValue(payload, "amount");
        return value == null || value.isBlank() ? request.getAmount() : new java.math.BigDecimal(value);
    }
}
