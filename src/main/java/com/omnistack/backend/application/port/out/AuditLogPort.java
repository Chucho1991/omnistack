package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.TransactionAuditLog;

/**
 * Puerto de persistencia de auditoria funcional.
 */
public interface AuditLogPort {

    /**
     * Persiste un evento de auditoria funcional.
     *
     * @param log evento de auditoria a registrar
     */
    void save(TransactionAuditLog log);
}
