package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Subcategoria de negocio expuesta por el endpoint de business lines.
 */
@Value
@Builder
@Schema(description = "Subcategoria comercial agrupada por categoria y proveedor")
public class BusinessLineCollectionSubcategoryResponse {
    @JsonProperty("category_code")
    String categoryCode;
    @JsonProperty("category_name")
    String categoryName;
    @JsonProperty("subcategory_code")
    String subcategoryCode;
    @JsonProperty("subcategory_name")
    String subcategoryName;
    @JsonProperty("is_active")
    boolean active;
    @JsonProperty("service_providers")
    List<BusinessLineProviderResponse> serviceProviders;
}
