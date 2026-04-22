package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Proveedor habilitado dentro de una subcategoria comercial.
 */
@Value
@Builder
@Schema(description = "Proveedor comercial parametrizado")
public class BusinessLineProviderResponse {
    @JsonProperty("service_provider_code")
    String serviceProviderCode;
    @JsonProperty("provider_name")
    String providerName;
    @JsonProperty("is_active")
    boolean active;
    List<BusinessLineServiceResponse> services;
}
