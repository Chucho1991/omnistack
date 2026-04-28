package com.omnistack.backend.shared.constants;

/**
 * Codigos de error estandarizados.
 */
public final class ErrorCodes {

    /**
     * Error por validacion de entrada.
     */
    public static final String VALIDATION_ERROR = "VAL-001";
    /**
     * Error funcional de negocio.
     */
    public static final String BUSINESS_ERROR = "BUS-001";
    /**
     * Error en integracion externa.
     */
    public static final String INTEGRATION_ERROR = "INT-001";
    /**
     * Error por catalogo no encontrado.
     */
    public static final String CATALOG_NOT_FOUND = "CAT-404";
    /**
     * Error interno no controlado.
     */
    public static final String INTERNAL_ERROR = "GEN-500";

    private ErrorCodes() {
    }
}
