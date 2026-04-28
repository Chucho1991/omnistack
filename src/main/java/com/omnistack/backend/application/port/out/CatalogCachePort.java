package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.CatalogSnapshot;
import com.omnistack.backend.domain.model.ServiceDefinition;
import java.util.List;
import java.util.Optional;

/**
 * Acceso al catalogo vigente en memoria.
 */
public interface CatalogCachePort {

    /**
     * Obtiene el snapshot vigente del catalogo.
     *
     * @return snapshot actual en memoria
     */
    CatalogSnapshot getCurrentSnapshot();

    /**
     * Busca un servicio por sus codigos comerciales.
     *
     * @param categoryCode codigo de categoria
     * @param subcategoryCode codigo de subcategoria
     * @param providerCode codigo de proveedor
     * @param rmsItemCode codigo RMS del item
     * @return servicio encontrado, si existe
     */
    Optional<ServiceDefinition> findService(String categoryCode, String subcategoryCode, String providerCode, String rmsItemCode);

    /**
     * Busca servicios por tipo de movimiento.
     *
     * @param movementType tipo de movimiento o null para todos
     * @return servicios que cumplen el filtro
     */
    List<ServiceDefinition> findServicesByMovementType(String movementType);
}
