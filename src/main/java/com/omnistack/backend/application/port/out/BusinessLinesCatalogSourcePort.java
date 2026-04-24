package com.omnistack.backend.application.port.out;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.domain.model.CatalogSnapshot;

/**
 * Puerto de salida para cargar el catalogo de business lines segun el contexto del POS.
 */
public interface BusinessLinesCatalogSourcePort {

    /**
     * Carga el catalogo correspondiente al request funcional del endpoint.
     *
     * @param request request del endpoint business-lines
     * @return snapshot del catalogo aplicable al POS solicitado
     */
    CatalogSnapshot loadCatalogSnapshot(BusinessLinesRequest request);
}
