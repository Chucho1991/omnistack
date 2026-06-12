package com.omnistack.backend.domain.enums;

/**
 * Tipo de movimiento transaccional soportado por la API.
 */
public enum MovementType {
    /**
     * Movimiento de ingreso de fondos.
     */
    CASH_IN,
    /**
     * Movimiento de salida de fondos.
     */
    CASH_OUT,
    /**
     * Creacion de ticket de apuesta (Pega3).
     */
    CREATE_TICKET
}
