package com.omnistack.backend.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Subcategoria funcional de recaudo o transaccion.
 */
@Value
@Builder(toBuilder = true)
public class CollectionSubcategory {
    @JsonProperty("subcategory_code")
    String subcategoryCode;
    @JsonProperty("subcategory_name")
    String subcategoryName;
    @JsonProperty("is_active")
    boolean active;
    List<ServiceProvider> providers;
}
