package com.omnistack.backend.domain.enums;

/**
 * Estados posibles de un registro de cupo CASH_OUT.
 */
public enum CashOutQuotaStatus {
    /**
     * Reserva creada en el precheck; descuenta cupo disponible.
     */
    RESERVADO,
    /**
     * Reserva confirmada por el execute; consumo definitivo del cupo.
     */
    CONFIRMADO,
    /**
     * Reserva expirada por timeout (no se confirmo en el tiempo configurado).
     * El cupo se restituye al saldo disponible del local.
     */
    EXPIRADO,
    /**
     * Reserva revertida por un reverse exitoso en el mismo dia.
     * El cupo se restituye al saldo disponible del local.
     */
    REVERTIDO
}
