package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.ItemConfigDiagnosticResponse;
import com.omnistack.backend.application.service.ItemConfigDiagnosticService;
import com.omnistack.backend.shared.constants.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de administracion para diagnostico de parametrizacion de items.
 * Permite consultar toda la configuracion asociada a un rms_item_code
 * y detectar automaticamente campos faltantes.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Admin - Diagnostico", description = "Herramientas de diagnostico de parametrizacion")
public class ItemConfigDiagnosticController {

    private final ItemConfigDiagnosticService diagnosticService;

    /**
     * Retorna toda la parametrizacion asociada a un item, clasificada por tabla de origen,
     * junto con un diagnostico automatico de problemas detectados.
     *
     * @param rmsItemCode codigo del item RMS a consultar
     * @return diagnostico completo de parametrizacion
     */
    @GetMapping(ApiPaths.V1_ADMIN_ITEM_CONFIG)
    @Operation(
            summary = "Diagnostico de parametrizacion de un item",
            description = "Consulta todas las tablas involucradas en la parametrizacion de un rms_item_code "
                    + "(AD_SERVICIO_PARAMETROS, ITEM_MASTER, ITEM_SUPPLIER, IN_OMNI_PROVEEDOR_CONFIG, "
                    + "IN_OMNI_PROVEEDOR_WS, IN_OMNI_PROVEEDOR_WS_DEFS, IN_OMNI_INPUT_FIELDS) "
                    + "y genera un diagnostico automatico de campos faltantes.",
            responses = {
                @ApiResponse(responseCode = "200", description = "Diagnostico generado exitosamente")
            })
    public ResponseEntity<ItemConfigDiagnosticResponse> diagnoseItem(
            @Parameter(description = "Codigo del item RMS", example = "100708848")
            @PathVariable("rmsItemCode") String rmsItemCode) {
        return ResponseEntity.ok(diagnosticService.diagnose(rmsItemCode));
    }
}
