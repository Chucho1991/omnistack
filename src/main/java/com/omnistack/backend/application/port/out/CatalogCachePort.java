package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.CatalogSnapshot;
import com.omnistack.backend.domain.model.ServiceDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Acceso al catalogo vigente en memoria.
 */
public interface CatalogCachePort {

    CatalogSnapshot getCurrentSnapshot();

    Optional<ServiceDefinition> findService(String categoryCode, String subcategoryCode, String providerCode, String rmsItemCode);

    List<ServiceDefinition> findServicesByMovementType(String movementType);
}
