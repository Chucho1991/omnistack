package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.HealthcheckResponse;
import com.omnistack.backend.application.port.in.HealthcheckUseCase;
import com.omnistack.backend.shared.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para verificacion basica de salud de la aplicacion.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Healthcheck", description = "Estado basico operativo de la API")
public class HealthcheckController {

    private final HealthcheckUseCase healthcheckUseCase;

    /**
     * Retorna el estado operativo basico de la aplicacion.
     *
     * @return estado actual de la API
     */
    @GetMapping(ApiPaths.HEALTHCHECK)
    @Operation(
            summary = "Healthcheck de la API",
            description = "Expone el estado basico de disponibilidad del servicio",
            responses = {
                @ApiResponse(responseCode = "200", description = "Servicio disponible")
            })
    public ResponseEntity<HealthcheckResponse> healthcheck() {
        return ResponseEntity.ok(healthcheckUseCase.getHealthcheck());
    }
}
