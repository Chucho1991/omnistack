package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Categoria comercial visible para el punto de venta.
 */
@Value
@Builder(toBuilder = true)
public class Category {
    @JsonProperty("category_code")
    String categoryCode;
    @JsonProperty("category_name")
    String categoryName;
    List<CollectionSubcategory> subcategories;
}
