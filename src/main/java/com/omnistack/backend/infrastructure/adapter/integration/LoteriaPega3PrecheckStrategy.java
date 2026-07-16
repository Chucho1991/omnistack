package com.omnistack.backend.infrastructure.adapter.integration;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.port.out.Pega3DrawQueryPort;
import com.omnistack.backend.application.port.out.Pega3ProductQueryPort;
import com.omnistack.backend.application.port.out.strategy.AbstractProviderStrategy;
import com.omnistack.backend.application.port.out.strategy.PrecheckStrategy;
import com.omnistack.backend.application.service.ProviderConfigService;
import com.omnistack.backend.application.service.ProviderWsDefsService;
import com.omnistack.backend.application.service.ProviderWsService;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.MovementType;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.domain.model.Pega3DrawQueryCommand;
import com.omnistack.backend.domain.model.Pega3ProductQueryCommand;
import com.omnistack.backend.domain.model.ServiceDefinition;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Estrategia de PRECHECK CASH_IN para Pega3.
 * Realiza dos llamadas: VentaProductos + ObtieneSorteosActivo.
 */
@Component
@RequiredArgsConstructor
public class LoteriaPega3PrecheckStrategy extends AbstractProviderStrategy implements PrecheckStrategy {

    private static final String PROVIDER_KEY = "pega3";
    private static final String PROVIDER_NAME = "Loteria Pega3";
    private static final String PRECHECK_SORTEO_KEY = "PRECHECK_SORTEO";

    private final Pega3ProductQueryPort pega3ProductQueryPort;
    private final Pega3DrawQueryPort pega3DrawQueryPort;
    private final ProviderConfigService providerConfigService;
    private final ProviderWsDefsService providerWsDefsService;
    private final ProviderWsService providerWsService;

    @Override
    public boolean supports(ServiceDefinition serviceDefinition, Capability capability) {
        AppProperties.ProviderProperties provider = findProviderProperties(providerConfigService, PROVIDER_KEY);
        return capability == Capability.PRECHECK
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
        String operationUrl = getRequiredOperationUrl(providerWsService, providerWsDefsService, PROVIDER_KEY, capability, serviceDefinition, PROVIDER_NAME);
        String sorteoUrl = providerWsService.findUrl(PROVIDER_KEY, toWsKey(PRECHECK_SORTEO_KEY, serviceDefinition.getMovementType())).orElse(null);

        Pega3ProductQueryCommand productCommand = Pega3ProductQueryCommand.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .build();

        ExternalTransactionResponse productResponse = pega3ProductQueryPort.queryProduct(productCommand, operationUrl);

        PrecheckResponse.GameData gameData = null;
        if (productResponse.isApproved()) {
            Map<String, Object> productPayload = productResponse.getPayload();
            gameData = PrecheckResponse.GameData.builder()
                    .entryTypes(getStringList(productPayload, "entry_types"))
                    .betAmountOptions(getBigDecimalList(productPayload, "bet_amount_options"))
                    .minCost(getBigDecimalValue(productPayload, "min_cost"))
                    .maxCost(getBigDecimalValue(productPayload, "max_cost"))
                    .futureDrawsLimit(getIntegerValue(productPayload, "future_draws_limit"))
                    .advanceDrawLimit(getIntegerValue(productPayload, "advance_draw_limit"))
                    .playTypes(getStringList(productPayload, "play_types"))
                    .prizeLiabilityThreshold(getBigDecimalValue(productPayload, "prize_liability_threshold"))
                    .build();
        }

        PrecheckResponse.ActiveDraw activeDraw = null;
        if (sorteoUrl != null && !sorteoUrl.isBlank()) {
            Pega3DrawQueryCommand drawCommand = Pega3DrawQueryCommand.builder()
                    .uuid(request.getUuid())
                    .chain(request.getChain())
                    .store(request.getStore())
                    .pos(request.getPos())
                    .channelPos(request.getChannelPos().name())
                    .categoryCode(request.getCategoryCode())
                    .subcategoryCode(request.getSubcategoryCode())
                    .serviceProviderCode(request.getServiceProviderCode())
                    .rmsItemCode(request.getRmsItemCode())
                    .build();

            ExternalTransactionResponse drawResponse = pega3DrawQueryPort.queryActiveDraw(drawCommand, sorteoUrl);
            if (drawResponse.isApproved()) {
                Map<String, Object> drawPayload = drawResponse.getPayload();
                activeDraw = PrecheckResponse.ActiveDraw.builder()
                        .drawNumber(getIntegerValue(drawPayload, "draw_number"))
                        .drawDate(getStringValue(drawPayload, "draw_date"))
                        .build();
            }
        }

        boolean isError = !productResponse.isApproved();
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
                .gameData(gameData)
                .activeDraw(activeDraw);

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(productResponse.getExternalCode())
                    .message(productResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(productResponse.getExternalCode(), "Precheck Pega3 completado"));
        }

        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof List<?>) {
            return (List<String>) value;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<BigDecimal> getBigDecimalList(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof List<?>) {
            return (List<BigDecimal>) value;
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> payload, String key) {
        return decimalValue(payload, key);
    }

    private Integer getIntegerValue(Map<String, Object> payload, String key) {
        return integerValue(payload, key);
    }

    private String getStringValue(Map<String, Object> payload, String key) {
        return stringValue(payload, key);
    }
}
