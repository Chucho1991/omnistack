package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.CatalogSnapshot;

/**
 * Puerto de salida para cargar catalogos desde una fuente externa.
 */
public interface CatalogSourcePort {

    /**
     * Carga un snapshot completo del catalogo desde la fuente configurada.
     *
     * @return snapshot cargado desde la fuente externa
     */
    CatalogSnapshot loadCatalogSnapshot();
}
