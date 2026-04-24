package com.omnistack.backend.shared.constants;

/**
 * Rutas expuestas por la API.
 */
public final class ApiPaths {

    public static final String HEALTHCHECK = "/healthcheck";
    public static final String BUSINESS_LINES = "/business-lines";
    public static final String V1_PRECHECK = "/v1/precheck";
    public static final String V1_EXECUTE = "/v1/execute";
    public static final String V1_VERIFY = "/v1/verify";
    public static final String V1_REVERSE = "/v1/reverse";
    public static final String V1_PROVIDER_TOKEN_REFRESH = "/v1/provider-token/refresh";

    private ApiPaths() {
    }
}
