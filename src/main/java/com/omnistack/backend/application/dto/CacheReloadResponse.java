package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Resultado de la recarga manual de caches de configuracion")
public class CacheReloadResponse extends BaseResponse {

    @JsonProperty("reloaded_at")
    @Schema(example = "2026-06-19T10:00:00-05:00")
    private OffsetDateTime reloadedAt;

    @JsonProperty("caches")
    @Schema(example = "[\"provider_config\",\"provider_ws\",\"provider_ws_defs\",\"ad_item_servicio\",\"catalog\",\"business_lines\"]")
    private List<String> caches;
}
