package com.omnistack.backend.domain.model;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Snapshot en memoria del catalogo funcional.
 */
@Value
@Builder(toBuilder = true)
public class CatalogSnapshot {
    List<Category> categories;
    List<ServiceDefinition> services;
    OffsetDateTime loadedAt;
    String version;
}
