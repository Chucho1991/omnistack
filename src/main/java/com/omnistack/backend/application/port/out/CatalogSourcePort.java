package com.omnistack.backend.application.port.out;

import com.omnistack.backend.domain.model.CatalogSnapshot;

/**
 * Puerto de salida para cargar catalogos desde una fuente externa.
 */
public interface CatalogSourcePort {

    CatalogSnapshot loadCatalogSnapshot();
}
