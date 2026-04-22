package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnistack.backend.domain.enums.ChannelPos;
import com.omnistack.backend.domain.enums.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Consulta de oferta comercial por punto de venta")
public class BusinessLinesRequest {
    @NotBlank
    @Schema(example = "001")
    private String chain;

    @NotBlank
    @Schema(example = "0001")
    private String store;

    @JsonProperty("store_name")
    @JsonAlias("storeName")
    @Schema(example = "Tienda Centro")
    private String storeName;

    @NotBlank
    @Schema(example = "POS-01")
    private String pos;

    @NotNull
    @JsonProperty("channel_POS")
    @JsonAlias("channelPos")
    @Schema(example = "POS")
    private ChannelPos channelPos;

    @JsonProperty("movement_type_filter")
    @JsonAlias("movementTypeFilter")
    @Schema(example = "CASH_IN")
    private MovementType movementTypeFilter;
}
