package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.ExecuteRequest;
import com.omnistack.backend.application.dto.ExecuteResponse;
import com.omnistack.backend.application.dto.PrecheckRequest;
import com.omnistack.backend.application.dto.PrecheckResponse;
import com.omnistack.backend.application.dto.ReverseRequest;
import com.omnistack.backend.application.dto.ReverseResponse;
import com.omnistack.backend.application.dto.VerifyRequest;
import com.omnistack.backend.application.dto.VerifyResponse;
import com.omnistack.backend.application.port.in.TransactionUseCase;
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
 * Controller REST para orquestacion transaccional.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Transactions", description = "Endpoints transaccionales internos")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    /**
     * Ejecuta la validacion previa de una transaccion antes de su procesamiento.
     *
     * @param request datos de entrada requeridos para el precheck transaccional
     * @return respuesta HTTP con el resultado del precheck
     */
    @PostMapping(ApiPaths.V1_PRECHECK)
    @Operation(summary = "Precheck transaccional", responses = {
        @ApiResponse(responseCode = "200", description = "Precheck exitoso"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = com.omnistack.backend.shared.exception.ApiErrorResponse.class)))
    })
    public ResponseEntity<PrecheckResponse> precheck(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = @ExampleObject(value = ApiExamples.TRANSACTION_REQUEST)))
            @Valid @RequestBody PrecheckRequest request) {
        return ResponseEntity.ok(transactionUseCase.precheck(request));
    }

    /**
     * Ejecuta una transaccion sobre el flujo configurado para el proveedor.
     *
     * @param request datos de entrada requeridos para la ejecucion transaccional
     * @return respuesta HTTP con el resultado de la ejecucion
     */
    @PostMapping(ApiPaths.V1_EXECUTE)
    @Operation(summary = "Ejecucion transaccional", responses = {
        @ApiResponse(responseCode = "200", description = "Ejecucion exitosa"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = com.omnistack.backend.shared.exception.ApiErrorResponse.class)))
    })
    public ResponseEntity<ExecuteResponse> execute(@Valid @RequestBody ExecuteRequest request) {
        return ResponseEntity.ok(transactionUseCase.execute(request));
    }

    /**
     * Consulta el estado de una transaccion previamente procesada.
     *
     * @param request datos de entrada requeridos para la verificacion transaccional
     * @return respuesta HTTP con el resultado de la verificacion
     */
    @PostMapping(ApiPaths.V1_VERIFY)
    @Operation(summary = "Verificacion transaccional", responses = {
        @ApiResponse(responseCode = "200", description = "Verificacion exitosa"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = com.omnistack.backend.shared.exception.ApiErrorResponse.class)))
    })
    public ResponseEntity<VerifyResponse> verify(@Valid @RequestBody VerifyRequest request) {
        return ResponseEntity.ok(transactionUseCase.verify(request));
    }

    /**
     * Solicita el reverso de una transaccion procesada previamente.
     *
     * @param request datos de entrada requeridos para el reverso transaccional
     * @return respuesta HTTP con el resultado del reverso
     */
    @PostMapping(ApiPaths.V1_REVERSE)
    @Operation(summary = "Reverso transaccional", responses = {
        @ApiResponse(responseCode = "200", description = "Reverso exitoso"),
        @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = com.omnistack.backend.shared.exception.ApiErrorResponse.class)))
    })
    public ResponseEntity<ReverseResponse> reverse(@Valid @RequestBody ReverseRequest request) {
        return ResponseEntity.ok(transactionUseCase.reverse(request));
    }
}
