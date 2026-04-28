package com.omnistack.backend.domain.enums;

/**
 * Estados funcionales disponibles para una transaccion.
 */
public enum TransactionStatus {
    /**
     * Transaccion exitosa.
     */
    SUCCESS,
    /**
     * Transaccion fallida.
     */
    FAILED,
    /**
     * Transaccion pendiente.
     */
    PENDING
}
