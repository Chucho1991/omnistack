package com.omnistack.backend.infrastructure.adapter.audit;

import com.omnistack.backend.application.port.out.AuditLogPort;
import com.omnistack.backend.domain.model.TransactionAuditLog;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Repositorio temporal de auditoria en memoria.
 */
@Slf4j
@Component
public class InMemoryAuditLogAdapter implements AuditLogPort {

    @Getter
    private final List<TransactionAuditLog> entries = new CopyOnWriteArrayList<>();

    @Override
    public void save(TransactionAuditLog logEntry) {
        entries.add(logEntry);
        log.info("Audit entry stored for uuid={} endpoint={}", logEntry.getUuid(), logEntry.getEndpointInvoked());
    }
}
