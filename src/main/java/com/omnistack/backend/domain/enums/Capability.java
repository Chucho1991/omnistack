package com.omnistack.backend.domain.enums;

/**
 * Capacidades funcionales habilitadas para proveedores externos.
 */
public enum Capability {
    /**
     * Capacidad de validacion previa.
     */
    PRECHECK,
    /**
     * Capacidad de creacion de ticket.
     */
    CREATE_TICKET,
    /**
     * Capacidad de ejecucion.
     */
    EXECUTE,
    /**
     * Capacidad de verificacion.
     */
    VERIFY,
    /**
     * Capacidad de reverso.
     */
    REVERSE,
    /**
     * Capacidad de conciliacion.
     */
    CONCILIATE
}
