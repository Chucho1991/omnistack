package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Proveedor comercial o integrador asociado al servicio.
 */
@Value
@Builder(toBuilder = true)
public class ServiceProvider {
    @JsonProperty("service_provider_code")
    String serviceProviderCode;
    @JsonProperty("ruc_provider")
    String rucProvider;
    @JsonProperty("provider_name")
    String providerName;
    @JsonProperty("is_active")
    boolean active;
    List<ServiceDefinition> services;
}
