package com.omnistack.backend.application.mapper;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.BusinessLineCollectionSubcategoryResponse;
import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.constants.StatusCodes;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Fabrica centralizada de responses internos.
 */
public final class ResponseFactory {

    private ResponseFactory() {
    }

    /**
     * Construye la respuesta de lineas de negocio.
     *
     * @param request request original de consulta
     * @param collectionSubcategory subcategorias comerciales agrupadas
     * @return respuesta consolidada de lineas de negocio
     */
    public static BusinessLinesResponse businessLines(
            BusinessLinesRequest request,
            java.util.List<BusinessLineCollectionSubcategoryResponse> collectionSubcategory) {
        return BusinessLinesResponse.builder()
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .channelPos(request.getChannelPos().name())
                .collectionSubcategory(collectionSubcategory)
                .build();
    }

    /**
     * Construye una respuesta transaccional segun la capacidad procesada.
     *
     * @param request request transaccional original
     * @param externalResponse respuesta canonica del proveedor
     * @param capability capacidad procesada
     * @return respuesta transaccional interna
     * @throws IllegalArgumentException cuando la capacidad no esta soportada por la fabrica
     */
    public static BaseTransactionResponse transactionResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Capability capability) {
        return switch (capability) {
            case PRECHECK -> precheckResponse(request, externalResponse);
            case EXECUTE -> ExecuteResponse.builder()
                    .uuid(request.getUuid())
                    .errorFlag(false)
                    .status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"))
                    .build();
            case VERIFY -> VerifyResponse.builder()
                    .uuid(request.getUuid())
                    .errorFlag(false)
                    .status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"))
                    .build();
            case REVERSE -> ReverseResponse.builder()
                    .uuid(request.getUuid())
                    .errorFlag(false)
                    .status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"))
                    .build();
            default -> throw new IllegalArgumentException("Capability no soportada: " + capability);
        };
    }

    private static PrecheckResponse precheckResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        Integer providerError = integerValue(payload, "error");
        boolean isError = providerError != null && providerError != 0;

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
                .authorization(resolveAuthorization(payload))
                .serialnumber(stringValue(payload, "serialnumber"))
                .userid(stringValue(payload, "userid"))
                .document(stringValue(payload, "document"))
                .amount(decimalValue(payload, "amount"));

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(externalResponse.getExternalCode())
                    .message(externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.status(new StatusDetail(externalResponse.getExternalCode(), "Transacci\u00F3n correcta"));
        }

        return builder.build();
    }

    private static String resolveAuthorization(Map<String, Object> payload) {
        String authorization = stringValue(payload, "authorization");
        return authorization != null && !authorization.isBlank()
                ? authorization
                : "AUTO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private static String stringValue(Map<String, Object> payload, String key) {
        if (payload == null) {
            return null;
        }
        Object value = payload.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static BigDecimal decimalValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    private static Integer integerValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }
}
