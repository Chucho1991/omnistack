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
import com.omnistack.backend.application.port.out.ClaroExecutePort;
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
import com.omnistack.backend.domain.model.ClaroExecuteCommand;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia EXECUTE CASH_IN para CLARO. Llama a processRechargeRetail.
 */
@Component
@RequiredArgsConstructor
public class ClaroExecuteStrategy extends AbstractProviderStrategy implements ExecuteStrategy {

    private static final String PROVIDER_KEY = "claro";
    private static final String PROVIDER_NAME = "CLARO";

    private final ClaroExecutePort claroExecutePort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        return capability == Capability.EXECUTE
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && serviceDefinition.getSubcategoryCode() != null
                && serviceDefinition.getSubcategoryCode().equalsIgnoreCase(provider.getSubcategoryCode())
                && providerWsService.hasUrl(PROVIDER_KEY, wsKey)
                && providerWsDefsService.getOfferIds(PROVIDER_KEY, wsKey).containsKey(serviceDefinition.getRmsItemCode());
    }

    @Override
    public BaseTransactionResponse process(
            BaseTransactionRequest request,
            ServiceDefinition serviceDefinition,
            Capability capability) {
        AppProperties.ProviderProperties provider = getProviderProperties(providerConfigService, PROVIDER_KEY, PROVIDER_NAME);
        validateBusinessContext(request, serviceDefinition, provider);

        if (request.getPhone() == null || request.getPhone().isBlank()) {
            throw new IntegrationException("CLARO requiere el campo phone (numero de celular a recargar)");
        }
        if (request.getAmount() == null) {
            throw new IntegrationException("CLARO requiere el campo amount");
        }
        if (request.getAuthorization() == null || request.getAuthorization().isBlank()) {
            throw new IntegrationException("CLARO requiere authorization (AUTHORIZATIONNUMBER del PRECHECK)");
        }

        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        String operationUrl = providerWsService.requireUrl(PROVIDER_KEY, wsKey, PROVIDER_NAME);
        String offerId = resolveOfferId(providerWsDefsService.getOfferIds(PROVIDER_KEY, wsKey), request.getRmsItemCode());
        String amount = formatAmount(request.getAmount());

        ClaroExecuteCommand command = ClaroExecuteCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(request.getRmsItemCode())
                .phone(request.getPhone())
                .amount(amount)
                .offerId(offerId)
                .authorizationNumber(request.getAuthorization())
                .build();

        ExternalTransactionResponse externalResponse = claroExecutePort.processRecharge(command, operationUrl);
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
                .amount(request.getAmount());

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(CanonicalErrorCodeMapper.resolve(externalResponse))
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, "Recarga CLARO completada"));
        }

        return builder.build();
    }

    private String resolveOfferId(java.util.Map<String, String> offerIds, String rmsItemCode) {
        String offerId = offerIds.get(rmsItemCode);
        if (offerId == null || offerId.isBlank()) {
            throw new IntegrationException(
                    "CLARO no tiene OFFERID configurado para rms_item_code=" + rmsItemCode);
        }
        return offerId;
    }

    private static String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
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
