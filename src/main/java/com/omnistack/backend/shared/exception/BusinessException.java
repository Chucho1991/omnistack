package com.omnistack.backend.shared.exception;

/**
 * Excepcion para errores de negocio controlados por la aplicacion.
 */
public class BusinessException extends RuntimeException {

    /**
     * Construye una excepcion de negocio.
     *
     * @param message mensaje funcional de error.
     */
    public BusinessException(String message) {
        super(message);
    }
}
