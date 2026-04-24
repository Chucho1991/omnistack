package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * Respuesta del refresco manual de token de proveedor.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Resultado del refresco manual de token")
public class ProviderTokenRefreshResponse extends BaseResponse {

    @JsonProperty("category_code")
    @Schema(example = "1")
    private String categoryCode;

    @JsonProperty("subcategory_code")
    @Schema(example = "1")
    private String subcategoryCode;

    @JsonProperty("service_provider_code")
    @Schema(example = "2")
    private String serviceProviderCode;

    @JsonProperty("provider_name")
    @Schema(example = "LOTERIA NACIONAL")
    private String providerName;

    @JsonProperty("refreshed_at")
    @Schema(example = "2026-04-24T11:00:00-05:00")
    private OffsetDateTime refreshedAt;

    @JsonProperty("expires_at")
    @Schema(example = "2026-04-25T11:00:00-05:00")
    private OffsetDateTime expiresAt;
}
