package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.TransactionStatus;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Value;

/**
 * Registro transversal de auditoria funcional.
 */
@Value
@Builder(toBuilder = true)
public class TransactionAuditLog {
    String uuid;
    String endpointInvoked;
    String externalProvider;
    String internalRequest;
    String internalResponse;
    String externalRequest;
    String externalResponse;
    TransactionStatus status;
    String errorCode;
    String errorMessage;
    OffsetDateTime startTime;
    OffsetDateTime endTime;
    Long durationMs;
    String technicalUser;
    OffsetDateTime createdAt;
}
