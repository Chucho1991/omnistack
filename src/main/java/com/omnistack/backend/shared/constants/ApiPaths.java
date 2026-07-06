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
    public static final String V1_PRECHECK = "/v1/preCheck";
    /**
     * Alias lowercase del endpoint de prevalidacion (compatibilidad con front-ends que usan minusculas).
     */
    public static final String V1_PRECHECK_LOWER = "/v1/precheck";
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
     * Endpoint de creacion de ticket de apuesta (Pega3).
     */
    public static final String V1_CREATE_TICKET = "/v1/createTicket";
    /**
     * Alias lowercase del endpoint de creacion de ticket (compatibilidad con front-ends que usan minusculas).
     */
    public static final String V1_CREATE_TICKET_LOWER = "/v1/createticket";
    /**
     * Endpoint de refresco manual de token de proveedor.
     */
    public static final String V1_PROVIDER_TOKEN_REFRESH = "/v1/provider-token/refresh";
    /**
     * Endpoint de recarga manual de caches de configuracion.
     */
    public static final String V1_CACHE_RELOAD = "/v1/cache/reload";
    /**
     * Endpoint de descarga de comprobantes de venta (PDF).
     */
    public static final String V1_COMPROBANTES = "/v1/comprobantes";

    private ApiPaths() {
    }
}
