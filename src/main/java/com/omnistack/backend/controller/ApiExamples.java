package com.omnistack.backend.controller;

/**
 * Ejemplos JSON reutilizables para OpenAPI.
 */
public final class ApiExamples {

    /**
     * Request transaccional usado como ejemplo en Swagger.
     */
    public static final String TRANSACTION_REQUEST = """
            {
              "uuid":"f0908f64-9145-45cf-a22c-c36bca604372",
              "chain":"1",
              "store":"148",
              "store_name":"FYBECA AMAZONAS",
              "pos":"1",
              "channel_POS":"POS",
              "category_code":"1",
              "subcategory_code":"1",
              "service_provider_code":"2",
              "rms_item_code":"10001565828",
              "document":"0901111112",
              "amount":9.99
            }
            """;

    /**
     * Request de refresco manual de token usado como ejemplo en Swagger.
     */
    public static final String PROVIDER_TOKEN_REFRESH_REQUEST = """
            {
              "category_code":"1",
              "subcategory_code":"1",
              "service_provider_code":"2"
            }
            """;

    private ApiExamples() {
    }
}
