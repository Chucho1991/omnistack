package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.out.ClaroPrecheckPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.AdItemServicioService;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ClaroPrecheckCommand;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.ServiceDefinition;
import com.omnistack.backend.shared.constants.StatusCodes;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClaroPrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "claro";
    private static final String PROVIDER_NAME = "CLARO";

    private final ClaroPrecheckPort claroPrecheckPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;
    private final AdItemServicioService adItemServicioService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        return capability == Capability.PRECHECK
                && provider != null
                && serviceDefinition.getMovementType() == MovementType.CASH_IN
                && serviceDefinition.getServiceProviderCode() != null
                && serviceDefinition.getServiceProviderCode().equalsIgnoreCase(provider.getServiceProviderCode())
                && serviceDefinition.getSubcategoryCode() != null
                && serviceDefinition.getSubcategoryCode().equalsIgnoreCase(provider.getSubcategoryCode())
                && providerWsService.hasUrl(PROVIDER_KEY, wsKey)
                && adItemServicioService.hasTag(serviceDefinition.getRmsItemCode(), "OFFERID");
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

        String wsKey = toWsKey(capability.name(), serviceDefinition.getMovementType());
        String operationUrl = providerWsService.requireUrl(PROVIDER_KEY, wsKey, PROVIDER_NAME);
        String rmsItemCode = request.getRmsItemCode();
        String offerId = adItemServicioService.requireTag(rmsItemCode, "OFFERID", PROVIDER_NAME);
        String externalOperation = adItemServicioService.requireTag(rmsItemCode, "EXTERNALOPERATION", PROVIDER_NAME);
        String amount = formatAmount(request.getAmount());

        ClaroPrecheckCommand command = ClaroPrecheckCommand.builder()
                .uuid(request.getUuid()).chain(request.getChain()).store(request.getStore())
                .storeName(request.getStoreName()).pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode()).subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode()).rmsItemCode(rmsItemCode)
                .phone(request.getPhone())
                .amount(amount)
                .offerId(offerId)
                .companyId(providerConfigService.mapValue(PROVIDER_KEY, "company_id", request.getChain()))
                .externalOperation(externalOperation)
                .mediaId(providerConfigService.getString(PROVIDER_KEY, "media_id"))
                .codCaja(providerWsDefsService.getString(PROVIDER_KEY, wsKey, "cod_caja"))
                .codSite(providerWsDefsService.getString(PROVIDER_KEY, wsKey, "cod_site"))
                .build();

        ExternalTransactionResponse externalResponse = claroPrecheckPort.validateRecharge(command, operationUrl);
        return buildResponse(request, externalResponse);
    }

    private PrecheckResponse buildResponse(BaseTransactionRequest request, ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        boolean isError = !externalResponse.isApproved();

        PrecheckResponse.PrecheckResponseBuilder<?, ?> builder = PrecheckResponse.builder()
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
            builder.status(new StatusDetail(StatusCodes.SUCCESS, "Validacion CLARO completada"));
        }

        return builder.build();
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
