package com.omnistack.backend.application.service;

import com.omnistack.backend.application.port.out.AuditLogPort;
import com.omnistack.backend.domain.model.TransactionAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Servicio de auditoria funcional.
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogPort auditLogPort;

    /**
     * Registra un evento de auditoria funcional.
     *
     * @param log evento de auditoria a registrar
     */
    public void save(TransactionAuditLog log) {
        auditLogPort.save(log);
    }
}
