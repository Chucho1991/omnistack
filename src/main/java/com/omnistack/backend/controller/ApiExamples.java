package com.omnistack.backend.controller;

/**
 * Ejemplos JSON reutilizables para OpenAPI.
 */
public final class ApiExamples {

    public static final String TRANSACTION_REQUEST = """
            {
              "uuid":"f0908f64-9145-45cf-a22c-c36bca604372",
              "chain":"001",
              "store":"0001",
              "store_name":"Tienda Centro",
              "pos":"POS-01",
              "channel_POS":"POS",
              "movement_type":"CASH_IN",
              "category_code":"REC",
              "subcategory_code":"CEL",
              "service_provider_code":"CLARO",
              "rms_item_code":"900001",
              "amount":25.50,
              "phone":"0999999999"
            }
            """;

    private ApiExamples() {
    }
}
