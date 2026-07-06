package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.CacheReloadResponse;
import com.omnistack.backend.application.dto.ErrorResponse;
import com.omnistack.backend.application.port.in.CacheAdministrationUseCase;
import com.omnistack.backend.shared.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Cache", description = "Administracion de caches de configuracion")
public class CacheAdministrationController {

    private final CacheAdministrationUseCase cacheAdministrationUseCase;

    @PostMapping(ApiPaths.V1_CACHE_RELOAD)
    @Operation(summary = "Recarga manual de todos los caches de configuracion", responses = {
        @ApiResponse(responseCode = "200", description = "Caches recargados correctamente"),
        @ApiResponse(responseCode = "500", description = "Error al recargar caches", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CacheReloadResponse> reloadAll() {
        return ResponseEntity.ok(cacheAdministrationUseCase.reloadAll());
    }
}
