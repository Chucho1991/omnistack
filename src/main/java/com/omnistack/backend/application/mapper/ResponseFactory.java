package com.omnistack.backend.application.mapper;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.BusinessLineCollectionSubcategoryResponse;
import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.model.ExternalTransactionResponse;
import com.omnistack.backend.shared.constants.StatusCodes;

/**
 * Fabrica centralizada de responses internos.
 */
public final class ResponseFactory {

    private ResponseFactory() {
    }

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

    public static BaseTransactionResponse transactionResponse(
            BaseTransactionRequest request,
            ExternalTransactionResponse externalResponse,
            Capability capability) {
        StatusDetail status = new StatusDetail(StatusCodes.SUCCESS, capability.name() + " completado correctamente");
        return switch (capability) {
            case PRECHECK -> PrecheckResponse.builder()
                    .uuid(request.getUuid())
                    .transactionId(request.getUuid())
                    .providerCode(externalResponse.getExternalCode())
                    .providerMessage(externalResponse.getExternalMessage())
                    .isError(false)
                    .status(status)
                    .build();
            case EXECUTE -> ExecuteResponse.builder()
                    .uuid(request.getUuid())
                    .transactionId(request.getUuid())
                    .providerCode(externalResponse.getExternalCode())
                    .providerMessage(externalResponse.getExternalMessage())
                    .isError(false)
                    .status(status)
                    .build();
            case VERIFY -> VerifyResponse.builder()
                    .uuid(request.getUuid())
                    .transactionId(request.getUuid())
                    .providerCode(externalResponse.getExternalCode())
                    .providerMessage(externalResponse.getExternalMessage())
                    .isError(false)
                    .status(status)
                    .build();
            case REVERSE -> ReverseResponse.builder()
                    .uuid(request.getUuid())
                    .transactionId(request.getUuid())
                    .providerCode(externalResponse.getExternalCode())
                    .providerMessage(externalResponse.getExternalMessage())
                    .isError(false)
                    .status(status)
                    .build();
            default -> throw new IllegalArgumentException("Capability no soportada: " + capability);
        };
    }
}
