package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.BaseTransactionRequest;
import com.omnistack.backend.application.dto.BaseTransactionResponse;
import com.omnistack.backend.application.dto.CreateTicketRequest;
import com.omnistack.backend.application.dto.CreateTicketResponse;
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
import com.omnistack.backend.application.port.out.RegistroTrxPort;
import com.omnistack.backend.domain.enums.Capability;
import com.omnistack.backend.domain.enums.TransactionStatus;
import com.omnistack.backend.domain.model.ProviderFlowSelection;
import com.omnistack.backend.domain.model.RegistroTrx;
import com.omnistack.backend.domain.model.TransactionAuditLog;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionOrchestrationService implements TransactionUseCase {

    private final ProviderFlowResolver providerFlowResolver;
    private final AuditLogService auditLogService;
    private final RegistroTrxPort registroTrxPort;

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

    @Override
    public CreateTicketResponse createTicket(CreateTicketRequest request) {
        return (CreateTicketResponse) process(request, Capability.CREATE_TICKET, "/v1/createTicket");
    }

    private Object process(BaseTransactionRequest request, Capability capability, String endpoint) {
        OffsetDateTime startTime = OffsetDateTime.now();
        var selection = providerFlowResolver.resolve(request, capability);
        request.setMovementType(selection.getServiceDefinition().getMovementType());

        try {
            Object response = selection.getStrategy().process(request, selection.getServiceDefinition(), capability);
            OffsetDateTime endTime = OffsetDateTime.now();

            auditLogService.save(buildAuditLog(request, response, selection, capability, endpoint,
                    TransactionStatus.SUCCESS, null, null, 200, startTime, endTime));

            saveRegistroTrxIfApplicable(request, response, selection, capability);

            return response;
        } catch (IntegrationException exception) {
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(buildAuditLog(request, null, selection, capability, endpoint,
                    TransactionStatus.FAILED, "INTEGRATION_ERROR", exception.getMessage(), 502, startTime, endTime));
            throw exception;
        } catch (RuntimeException exception) {
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(buildAuditLog(request, null, selection, capability, endpoint,
                    TransactionStatus.FAILED, "UNHANDLED", exception.getMessage(), 500, startTime, endTime));
            throw exception;
        }
    }

    private TransactionAuditLog buildAuditLog(
            BaseTransactionRequest request,
            Object response,
            ProviderFlowSelection selection,
            Capability capability,
            String endpoint,
            TransactionStatus status,
            String errorCode,
            String errorMessage,
            int httpStatus,
            OffsetDateTime startTime,
            OffsetDateTime endTime) {
        return TransactionAuditLog.builder()
                .uuid(request.getUuid())
                .endpointInvoked(endpoint)
                .externalProvider(selection.getServiceDefinition().getServiceProviderCode())
                .internalRequest(JsonUtil.toJsonSilently(request))
                .internalResponse(JsonUtil.toJsonSilently(response))
                .externalRequest("{\"delegated\":true}")
                .externalResponse("{\"delegated\":true}")
                .status(status)
                .errorCode(errorCode)
                .errorMessage(errorMessage)
                .startTime(startTime)
                .endTime(endTime)
                .durationMs(java.time.Duration.between(startTime, endTime).toMillis())
                .technicalUser("system")
                .createdAt(OffsetDateTime.now())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .canal(request.getChannelPos() != null ? request.getChannelPos().name() : null)
                .capability(capability.name())
                .httpStatus(httpStatus)
                .build();
    }

    private void saveRegistroTrxIfApplicable(
            BaseTransactionRequest request,
            Object response,
            ProviderFlowSelection selection,
            Capability capability) {
        if (!(response instanceof BaseTransactionResponse baseResponse) || baseResponse.isErrorFlag()) {
            return;
        }
        String authorization = null;
        BigDecimal monto = null;
        String codEstado = null;

        if (capability == Capability.EXECUTE && response instanceof ExecuteResponse er) {
            authorization = er.getAuthorization();
            monto = er.getAmount();
            codEstado = "OK";
        } else if (capability == Capability.CREATE_TICKET && response instanceof CreateTicketResponse ctr) {
            authorization = ctr.getAuthorization();
            codEstado = "OK";
        } else if (capability == Capability.REVERSE && response instanceof ReverseResponse rr) {
            authorization = rr.getAuthorization();
            monto = rr.getAmount();
            codEstado = "REVERTIDO";
        } else {
            return;
        }

        registroTrxPort.save(RegistroTrx.builder()
                .uuid(request.getUuid())
                .chain(request.getChain())
                .store(request.getStore())
                .storeName(request.getStoreName())
                .pos(request.getPos())
                .canal(request.getChannelPos() != null ? request.getChannelPos().name() : null)
                .proveedor(selection.getServiceDefinition().getServiceProviderCode())
                .categoryCode(request.getCategoryCode())
                .subcategoryCode(request.getSubcategoryCode())
                .serviceProviderCode(request.getServiceProviderCode())
                .rmsItemCode(request.getRmsItemCode())
                .capability(capability.name())
                .authorization(authorization)
                .monto(monto)
                .moneda("USD")
                .codEstado(codEstado)
                .build());
    }
}
