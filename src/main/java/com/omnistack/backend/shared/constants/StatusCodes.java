package com.omnistack.backend.shared.constants;

/**
 * Codigos de estado funcionales.
 */
public final class StatusCodes {

    /**
     * Estado funcional exitoso.
     */
    public static final String SUCCESS = "00";
    /**
     * Estado funcional de validacion fallida.
     */
    public static final String VALIDATION_FAILED = "01";
    /**
     * Estado funcional de rechazo de negocio.
     */
    public static final String BUSINESS_REJECTED = "02";
    /**
     * Estado funcional de falla de integracion.
     */
    public static final String INTEGRATION_FAILED = "03";
    /**
     * Estado funcional de catalogo no disponible.
     */
    public static final String CATALOG_UNAVAILABLE = "04";

    private StatusCodes() {
    }
}
