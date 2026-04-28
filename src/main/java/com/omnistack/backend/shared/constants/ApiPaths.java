package com.omnistack.backend.shared.constants;

/**
 * Rutas expuestas por la API.
 */
public final class ApiPaths {

    /**
     * Endpoint de healthcheck.
     */
    public static final String HEALTHCHECK = "/healthcheck";
    /**
     * Endpoint de consulta de lineas de negocio.
     */
    public static final String BUSINESS_LINES = "/business-lines";
    /**
     * Endpoint de prevalidacion transaccional.
     */
    public static final String V1_PRECHECK = "/v1/precheck";
    /**
     * Endpoint de ejecucion transaccional.
     */
    public static final String V1_EXECUTE = "/v1/execute";
    /**
     * Endpoint de verificacion transaccional.
     */
    public static final String V1_VERIFY = "/v1/verify";
    /**
     * Endpoint de reverso transaccional.
     */
    public static final String V1_REVERSE = "/v1/reverse";
    /**
     * Endpoint de refresco manual de token de proveedor.
     */
    public static final String V1_PROVIDER_TOKEN_REFRESH = "/v1/provider-token/refresh";

    private ApiPaths() {
    }
}
