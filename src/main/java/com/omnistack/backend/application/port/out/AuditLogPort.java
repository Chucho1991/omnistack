package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.TransactionAuditLog;

/**
 * Puerto de persistencia de auditoria funcional.
 */
public interface AuditLogPort {

    void save(TransactionAuditLog log);
}
