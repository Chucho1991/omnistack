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
import com.omnistack.backend.shared.util.CanonicalErrorCodeMapper;
import com.omnistack.backend.shared.validation.ExternalAmountValidation;
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
            case EXECUTE -> genericExecuteResponse(request, externalResponse, capability);
            case VERIFY -> genericVerifyResponse(request, externalResponse, capability);
            case REVERSE -> genericReverseResponse(request, externalResponse, capability);
            default -> throw new IllegalArgumentException("Capability no soportada: " + capability);
        };
    }

    private static ExecuteResponse genericExecuteResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Capability capability) {
        ExternalAmountValidation.Result amountValidation = ExternalAmountValidation.compare(request, externalResponse.getPayload());
        boolean isError = !externalResponse.isApproved() || amountValidation.hasMismatch();
        ExecuteResponse.ExecuteResponseBuilder<?, ?> builder = ExecuteResponse.builder()
                .uuid(request.getUuid())
                .errorFlag(isError)
                .amount(amountValidation.externalAmount());
        if (isError) {
            builder.error(genericError(externalResponse, amountValidation));
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"));
        }
        return builder.build();
    }

    private static VerifyResponse genericVerifyResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Capability capability) {
        ExternalAmountValidation.Result amountValidation = ExternalAmountValidation.compare(request, externalResponse.getPayload());
        boolean isError = !externalResponse.isApproved() || amountValidation.hasMismatch();
        VerifyResponse.VerifyResponseBuilder<?, ?> builder = VerifyResponse.builder()
                .uuid(request.getUuid())
                .errorFlag(isError);
        if (isError) {
            builder.error(genericError(externalResponse, amountValidation));
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"));
        }
        return builder.build();
    }

    private static ReverseResponse genericReverseResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Capability capability) {
        ExternalAmountValidation.Result amountValidation = ExternalAmountValidation.compare(request, externalResponse.getPayload());
        boolean isError = !externalResponse.isApproved() || amountValidation.hasMismatch();
        ReverseResponse.ReverseResponseBuilder<?, ?> builder = ReverseResponse.builder()
                .uuid(request.getUuid())
                .errorFlag(isError)
                .amount(amountValidation.externalAmount());
        if (isError) {
            builder.error(genericError(externalResponse, amountValidation));
        } else {
            builder.status(new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente"));
        }
        return builder.build();
    }

    private static ErrorDetail genericError(
            ExternalTransactionResponse externalResponse,
            ExternalAmountValidation.Result amountValidation) {
        return ErrorDetail.builder()
                .code(amountValidation.hasMismatch()
                        ? StatusCodes.VALIDATION_FAILED
                        : CanonicalErrorCodeMapper.resolve(externalResponse))
                .message(amountValidation.hasMismatch()
                        ? amountValidation.mismatchMessage()
                        : externalResponse.getExternalMessage())
                .build();
    }

    private static PrecheckResponse precheckResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse) {
        Map<String, Object> payload = externalResponse.getPayload();
        Integer providerError = integerValue(payload, "error");
        ExternalAmountValidation.Result amountValidation = ExternalAmountValidation.compare(request, payload);
        boolean isError = !externalResponse.isApproved()
                || providerError != null && providerError != 0
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
                .serialnumber(stringValue(payload, "serialnumber"))
                .userid(stringValue(payload, "userid"))
                .document(stringValue(payload, "document"))
                .amount(amountValidation.externalAmount());

        if (isError) {
            builder.error(ErrorDetail.builder()
                    .code(amountValidation.hasMismatch()
                            ? StatusCodes.VALIDATION_FAILED
                            : resolveCanonicalErrorCode(externalResponse))
                    .message(amountValidation.hasMismatch()
                            ? amountValidation.mismatchMessage()
                            : externalResponse.getExternalMessage())
                    .build());
        } else {
            builder.authorization(resolveAuthorization(payload))
                    .status(new StatusDetail(StatusCodes.SUCCESS, "Transacci\u00F3n correcta"));
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

    private static Integer integerValue(Map<String, Object> payload, String key) {
        String value = stringValue(payload, key);
        return value == null || value.isBlank() ? null : Integer.valueOf(value);
    }

    private static String resolveCanonicalErrorCode(ExternalTransactionResponse externalResponse) {
        return CanonicalErrorCodeMapper.resolve(externalResponse);
    }

}
