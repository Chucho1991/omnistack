package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;

/**
 * Puerto de entrada para consultar la oferta comercial.
 */
public interface BusinessLinesUseCase {

    /**
     * Consulta las lineas de negocio disponibles para un punto de venta.
     *
     * @param request datos de consulta del catalogo comercial
     * @return oferta comercial consolidada
     */
    BusinessLinesResponse getBusinessLines(BusinessLinesRequest request);
}
