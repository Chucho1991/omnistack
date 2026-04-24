package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud para refrescar manualmente el token de un proveedor.
 */
@Data
@NoArgsConstructor
@Schema(description = "Solicitud de refresco manual de token por proveedor")
public class ProviderTokenRefreshRequest {

    @NotBlank
    @JsonProperty("category_code")
    @JsonAlias("categoryCode")
    @Schema(example = "1")
    private String categoryCode;

    @NotBlank
    @JsonProperty("subcategory_code")
    @JsonAlias("subcategoryCode")
    @Schema(example = "1")
    private String subcategoryCode;

    @NotBlank
    @JsonProperty("service_provider_code")
    @JsonAlias("serviceProviderCode")
    @Schema(example = "2")
    private String serviceProviderCode;
}
