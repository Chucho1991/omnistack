package com.omnistack.backend.application.port.in;

import com.omnistack.backend.application.dto.HealthcheckResponse;

/**
 * Puerto de entrada para consultar el estado basico del servicio.
 */
public interface HealthcheckUseCase {

    /**
     * Obtiene el estado operativo actual de la API.
     *
     * @return respuesta de healthcheck
     */
    HealthcheckResponse getHealthcheck();
}
