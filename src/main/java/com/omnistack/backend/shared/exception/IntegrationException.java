package com.omnistack.backend.shared.exception;

/**
 * Excepcion para errores tecnicos durante integraciones externas.
 */
public class IntegrationException extends RuntimeException {

    /**
     * Crea una excepcion de integracion con mensaje funcional.
     *
     * @param message mensaje de error.
     */
    public IntegrationException(String message) {
        super(message);
    }

    /**
     * Crea una excepcion de integracion con causa raiz.
     *
     * @param message mensaje de error.
     * @param cause causa original de la falla.
     */
    public IntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
