package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Response del endpoint de consulta de lineas de negocio.
 */
@Value
@Builder
@Schema(description = "Oferta comercial consolidada disponible para el punto de venta")
public class BusinessLinesResponse {
    String chain;
    String store;
    @JsonProperty("store_name")
    String storeName;
    String pos;
    @JsonProperty("channel_POS")
    String channelPos;
    @JsonProperty("collection_subcategory")
    List<BusinessLineCollectionSubcategoryResponse> collectionSubcategory;
}
