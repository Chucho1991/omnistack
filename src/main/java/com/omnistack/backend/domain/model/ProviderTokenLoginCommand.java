package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Datos internos necesarios para solicitar un token dinamico a un proveedor.
 */
@Value
@Builder
public class ProviderTokenLoginCommand {
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String providerName;
    String baseUrl;
    String path;
    String username;
    String password;
    String productToSell;
}
