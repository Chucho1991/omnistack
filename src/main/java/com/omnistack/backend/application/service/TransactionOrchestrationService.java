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
import com.omnistack.backend.shared.exception.BusinessException;
import com.omnistack.backend.shared.exception.IntegrationException;
import com.omnistack.backend.shared.util.JsonUtil;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de orquestacion transaccional. Coordina la resolucion de estrategia,
 * ejecucion, auditoria, registro y homologacion de codigos de autorizacion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionOrchestrationService implements TransactionUseCase {

    private final ProviderFlowResolver providerFlowResolver;
    private final AuditLogService auditLogService;
    private final RegistroTrxPort registroTrxPort;
    private final HomologatedCodeService homologatedCodeService;
    private final CashOutQuotaService cashOutQuotaService;
    private final TransactionAmountValidationService transactionAmountValidationService;

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

        boolean isHomologated = selection.getServiceDefinition().isHomologatedAuth();
        boolean isCashOutQuota = cashOutQuotaService.appliesTo(selection.getServiceDefinition());

        // Control de monto maximo/minimo por transaccion (aplica a todos los items excepto CASH_OUT
        // que ya tiene su propia validacion de monto dentro del control de cupo diario)
        if ((capability == Capability.PRECHECK || capability == Capability.EXECUTE) && !isCashOutQuota) {
            validateTransactionAmount(request, selection);
        }

        // Control de cupo CASH_OUT: reservar cupo en PRECHECK antes de invocar al proveedor
        if (capability == Capability.PRECHECK && isCashOutQuota) {
            reserveCashOutQuota(request, selection);
        }

        // Para REVERSE con homologacion: resolver el authorization original del proveedor
        if (capability == Capability.REVERSE && isHomologated && request.getAuthorization() != null) {
            resolveOriginalAuthForReverse(request);
        }

        try {
            Object response = selection.getStrategy().process(request, selection.getServiceDefinition(), capability);
            OffsetDateTime endTime = OffsetDateTime.now();

            auditLogService.save(buildAuditLog(request, response, selection, capability, endpoint,
                    TransactionStatus.SUCCESS, null, null, 200, startTime, endTime));

            // Control de cupo CASH_OUT: confirmar reserva en EXECUTE exitoso
            if (capability == Capability.EXECUTE && isCashOutQuota) {
                confirmCashOutQuota(request, response);
            }

            // Control de cupo CASH_OUT: restituir cupo en REVERSE exitoso
            if (capability == Capability.REVERSE && isCashOutQuota) {
                revertCashOutQuota(request, response);
            }

            // Aplicar homologacion: generar codigo, persistir con original en AUTHORIZATION
            // y homologado en CP_VAR1, luego reemplazar authorization en el response para el POS
            saveRegistroTrxIfApplicable(request, response, selection, capability, isHomologated);

            return response;
        } catch (IntegrationException exception) {
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(buildAuditLog(request, null, selection, capability, endpoint,
                    TransactionStatus.FAILED, "INTEGRATION_ERROR", exception.getMessage(), 502, startTime, endTime));
            throw exception;
        } catch (BusinessException exception) {
            // BusinessException (ej: cupo insuficiente) — no se reintenta
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(buildAuditLog(request, null, selection, capability, endpoint,
                    TransactionStatus.FAILED, "BUSINESS_ERROR", exception.getMessage(), 422, startTime, endTime));
            throw exception;
        } catch (RuntimeException exception) {
            OffsetDateTime endTime = OffsetDateTime.now();
            auditLogService.save(buildAuditLog(request, null, selection, capability, endpoint,
                    TransactionStatus.FAILED, "UNHANDLED", exception.getMessage(), 500, startTime, endTime));
            throw exception;
        }
    }

    /**
     * Para REVERSE con homologacion: el POS envia el codigo homologado en el campo authorization.
     * Se resuelve el authorization original del proveedor desde IN_OMNI_REGISTRO_TRX y se reemplaza
     * en el request antes de invocar la estrategia del proveedor.
     *
     * @param request request de reverso con el codigo homologado
     */
    private void resolveOriginalAuthForReverse(BaseTransactionRequest request) {
        String homologatedCode = request.getAuthorization();
        registroTrxPort.findOriginalAuthByHomologatedCode(homologatedCode)
                .ifPresentOrElse(
                        originalAuth -> {
                            log.info("Homologacion REVERSE: codigo homologado='{}' -> authorization original='{}'",
                                    homologatedCode, originalAuth);
                            request.setAuthorization(originalAuth);
                        },
                        () -> log.warn("Homologacion REVERSE: no se encontro authorization original para "
                                + "codigo homologado='{}'. Se enviara el valor recibido al proveedor.", homologatedCode)
                );
    }

    /**
     * Valida que el monto de la transaccion este dentro del rango permitido
     * [MONTO_MIN, MONTO_MAX] configurado para el item. Lanza BusinessException si no cumple.
     */
    private void validateTransactionAmount(BaseTransactionRequest request, ProviderFlowSelection selection) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        transactionAmountValidationService.validate(amount, selection.getServiceDefinition());
    }

    /**
     * Reserva cupo CASH_OUT en el precheck. Si el cupo es insuficiente o el monto
     * excede el maximo, lanza BusinessException.
     */
    private void reserveCashOutQuota(BaseTransactionRequest request, ProviderFlowSelection selection) {
        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        cashOutQuotaService.reserveQuota(
                request.getUuid(),
                request.getChain(),
                request.getStore(),
                request.getPos(),
                request.getRmsItemCode(),
                request.getServiceProviderCode(),
                amount,
                selection.getServiceDefinition().getMaxAmount());
    }

    /**
     * Confirma la reserva de cupo CASH_OUT tras un execute exitoso.
     */
    private void confirmCashOutQuota(BaseTransactionRequest request, Object response) {
        if (response instanceof BaseTransactionResponse baseResponse && !baseResponse.isErrorFlag()) {
            cashOutQuotaService.confirmQuota(request.getUuid());
        }
    }

    /**
     * Restituye el cupo CASH_OUT tras un reverse exitoso (solo si es el mismo dia).
     */
    private void revertCashOutQuota(BaseTransactionRequest request, Object response) {
        if (response instanceof BaseTransactionResponse baseResponse && !baseResponse.isErrorFlag()) {
            cashOutQuotaService.revertQuota(request.getUuid());
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

    /**
     * Persiste el registro de transaccion y aplica homologacion de codigo de autorizacion
     * cuando corresponde.
     *
     * <p>Flujo con homologacion activa (EXECUTE/CREATE_TICKET):
     * <ol>
     *   <li>Genera codigo homologado unico</li>
     *   <li>Persiste en BD: AUTHORIZATION = codigo original del proveedor, CP_VAR1 = codigo homologado</li>
     *   <li>Reemplaza authorization en el response para que el POS reciba el codigo homologado</li>
     * </ol>
     *
     * @param request request original
     * @param response response de la estrategia
     * @param selection seleccion de flujo (contiene ServiceDefinition)
     * @param capability capacidad ejecutada
     * @param isHomologated true si el servicio tiene homologacion de autorizacion activa
     */
    private void saveRegistroTrxIfApplicable(
            BaseTransactionRequest request,
            Object response,
            ProviderFlowSelection selection,
            Capability capability,
            boolean isHomologated) {
        if (!(response instanceof BaseTransactionResponse baseResponse) || baseResponse.isErrorFlag()) {
            return;
        }
        String authorization = null;
        BigDecimal monto = null;
        String codEstado = null;
        String homologatedCode = null;

        if (capability == Capability.EXECUTE && response instanceof ExecuteResponse er) {
            authorization = er.getAuthorization();
            monto = er.getAmount();
            codEstado = "OK";

            // Homologacion: generar codigo, guardar original en BD, devolver homologado al POS
            if (isHomologated && authorization != null && !authorization.isBlank()) {
                homologatedCode = homologatedCodeService.generate();
                log.info("Homologacion EXECUTE: authorization original='{}' -> codigo homologado='{}'",
                        authorization, homologatedCode);
                // authorization mantiene el valor original para persistir en AUTHORIZATION
                // El response se actualiza para que el POS reciba el homologado
                er.setAuthorization(homologatedCode);
            }
        } else if (capability == Capability.CREATE_TICKET && response instanceof CreateTicketResponse ctr) {
            authorization = ctr.getAuthorization();
            codEstado = "OK";

            if (isHomologated && authorization != null && !authorization.isBlank()) {
                homologatedCode = homologatedCodeService.generate();
                log.info("Homologacion CREATE_TICKET: authorization original='{}' -> codigo homologado='{}'",
                        authorization, homologatedCode);
                ctr.setAuthorization(homologatedCode);
            }
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
                .cpVar1(homologatedCode)
                .build());
    }
}
