package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.application.port.in.TransactionUseCase;
import com.omnistack.backend.application.port.out.ProviderFlowResolver;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.TransactionStatus;
import com.omnistack.backend.domain.model.TransactionAuditLog;
import com.omnistack.backend.shared.util.JsonUtil;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Orquestador de operaciones transaccionales internas.
 */
@Service
@RequiredArgsConstructor
public class TransactionOrchestrationService implements TransactionUseCase {

    private final ProviderFlowResolver providerFlowResolver;
    private final AuditLogService auditLogService;

    @Override
    public PrecheckResponse precheck(PrecheckRequest request) {
        return (PrecheckResponse) process(request, Capability.PRECHECK, "/v1/precheck");
    }

    @Override
    public ExecuteResponse execute(ExecuteRequest request) {
        return (ExecuteResponse) process(request, Capability.EXECUTE, "/v1/execute");
    }

    @Override
    public VerifyResponse verify(VerifyRequest request) {
        return (VerifyResponse) process(request, Capability.VERIFY, "/v1/verify");
    }

    @Override
    public ReverseResponse reverse(ReverseRequest request) {
        return (ReverseResponse) process(request, Capability.REVERSE, "/v1/reverse");
    }

    private Object process(BaseTransactionRequest request, Capability capability, String endpoint) {
        OffsetDateTime startTime = OffsetDateTime.now();
        var selection = providerFlowResolver.resolve(request, capability);

        try {
            Object response = selection.getStrategy().process(request, selection.getServiceDefinition(), capability);
            OffsetDateTime endTime = OffsetDateTime.now();

            auditLogService.save(TransactionAuditLog.builder()
                    .uuid(request.getUuid())
                    .endpointInvoked(endpoint)
                    .externalProvider(selection.getServiceDefinition().getServiceProviderCode())
                    .internalRequest(JsonUtil.toJsonSilently(request))
                    .internalResponse(JsonUtil.toJsonSilently(response))
                    .externalRequest("{\"delegated\":true}")
                    .externalResponse("{\"delegated\":true}")
                    .status(TransactionStatus.SUCCESS)
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                    .technicalUser("system")
                    .createdAt(OffsetDateTime.now())
                    .build());
            return response;
        } catch (RuntimeException exception) {
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(TransactionAuditLog.builder()
                    .uuid(request.getUuid())
                    .endpointInvoked(endpoint)
                    .externalProvider(selection.getServiceDefinition().getServiceProviderCode())
                    .internalRequest(JsonUtil.toJsonSilently(request))
                    .status(TransactionStatus.FAILED)
                    .errorCode("UNHANDLED")
                    .errorMessage(exception.getMessage())
                    .startTime(startTime)
                    .endTime(endTime)
                    .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                    .technicalUser("system")
                    .createdAt(OffsetDateTime.now())
                    .build());
            throw exception;
        }
    }
}
