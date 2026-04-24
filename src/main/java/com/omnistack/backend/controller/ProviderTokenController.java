package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.dto.ProviderTokenRefreshResponse;
import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import com.omnistack.backend.shared.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST para administracion de tokens de proveedores.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Provider Tokens", description = "Administracion de tokens dinamicos de integracion")
public class ProviderTokenController {

    private final ProviderTokenAdministrationUseCase providerTokenAdministrationUseCase;

    /**
     * Fuerza la actualizacion del token de un proveedor configurado.
     *
     * @param request proveedor a refrescar
     * @return metadata del token actualizado
     */
    @PostMapping(ApiPaths.V1_PROVIDER_TOKEN_REFRESH)
    @Operation(summary = "Refresco manual de token por proveedor", responses = {
        @ApiResponse(responseCode = "200", description = "Token actualizado correctamente"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ErrorDetail.class))),
        @ApiResponse(responseCode = "422", description = "Proveedor no aplicable para refresh", content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
    })
    public ResponseEntity<ProviderTokenRefreshResponse> refreshToken(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = ApiExamples.PROVIDER_TOKEN_REFRESH_REQUEST)))
            @Valid @RequestBody ProviderTokenRefreshRequest request) {
        return ResponseEntity.ok(providerTokenAdministrationUseCase.refreshToken(request));
    }
}
