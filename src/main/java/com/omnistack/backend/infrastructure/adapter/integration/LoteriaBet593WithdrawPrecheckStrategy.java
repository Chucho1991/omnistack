package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.Bet593WithdrawValidationPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.Bet593WithdrawCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.shared.validation.ExternalAmountValidation;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Estrategia de PRECHECK CASH_OUT para consultar notas de retiro BET593 mediante Loteria Nacional.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class LoteriaBet593WithdrawPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "loteria";
    private static final String PROVIDER_NAME = "Loteria BET593";
    private static final String EXECUTED_WITHDRAW_CODE = "400022";

    private final Bet593WithdrawValidationPort bet593WithdrawValidationPort;
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
        validateBusinessContext(request, serviceDefinition, provider);
        validateRequiredRequestFields(request);
        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);

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

        ExternalTransactionResponse externalResponse = bet593WithdrawValidationPort.validateWithdraw(command, operationUrl);
        return buildResponse(request, externalResponse);
    }

    private PrecheckResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean executedWithdraw = EXECUTED_WITHDRAW_CODE.equals(externalResponse.getExternalCode());
        ExternalAmountValidation.Result amountValidation = ExternalAmountValidation.compare(request, payload);
        boolean isError = !executedWithdraw
                && (!externalResponse.isApproved()
                || stringValue(payload, "message") != null
                && !stringValue(payload, "message").isBlank())
                || amountValidation.hasMismatch();

        PrecheckResponse.PrecheckResponseBuilder<?, ?> builder = PrecheckResponse.builder()
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
                .serialnumber(resolveValue(payload, "serialnumber", request.getSerialnumber()))
                .userid(resolveValue(payload, "userid", request.getUserid()))
                .document(resolveValue(payload, "document", request.getDocument()))
                .amount(amountValidation.externalAmount());

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(amountValidation.hasMismatch()
                            ? StatusCodes.VALIDATION_FAILED
                            : CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(amountValidation.hasMismatch()
                            ? amountValidation.mismatchMessage()
                            : externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.authorization(resolveValue(payload, "authorization", request.getAuthorization()))
                    .status(new StatusDetail(StatusCodes.SUCCESS, "Transaccion correcta"));
        }

        return builder.build();
    }

    private void validateBusinessContext(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            AppProperties.ProviderProperties provider) {
        validateValue("category_code", request.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        // subcategory_code no se valida: el proveedor 'loteria' maneja dos subcategorias
        // (1120 CASH_IN, 1121 CASH_OUT) y solo puede almacenar una en IN_OMNI_PROVEEDOR_CONFIG.
        // El routing por los 4 campos del catalogo ya garantiza la correcta asignacion.
        validateValue("service_provider_code", request.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
        validateValue("category_code", serviceDefinition.getCategoryCode(), provider.getCategoryCode(), PROVIDER_NAME);
        validateValue("service_provider_code", serviceDefinition.getServiceProviderCode(), provider.getServiceProviderCode(), PROVIDER_NAME);
    }

    private void validateRequiredRequestFields(BaseTransactionRequest request) {
        if (request.getDocument() == null || request.getDocument().isBlank()) {
            throw new IntegrationException(PROVIDER_NAME + " requiere document para consultar nota de retiro");
        }
        if (request.getWithdrawId() == null || request.getWithdrawId().isBlank()) {
            throw new IntegrationException(PROVIDER_NAME + " requiere withdrawId para consultar nota de retiro");
        }
    }

}
