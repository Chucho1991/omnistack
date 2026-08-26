package com.omnistack.backend.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Categoria de negocio con sus subcategorias disponibles.
 */
@Value
@Builder
@Schema(description = "Categoria comercial con sus subcategorias agrupadas")
public class BusinessLineCategoryResponse {
    @JsonProperty("category_code")
    String categoryCode;
    @JsonProperty("category_name")
    String categoryName;
    List<BusinessLineCollectionSubcategoryResponse> subcategories;
}
