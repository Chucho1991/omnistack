package com.omnistack.backend.controller;

import com.omnistack.backend.application.dto.BusinessLinesRequest;
import com.omnistack.backend.application.dto.BusinessLinesResponse;
import com.omnistack.backend.application.dto.ErrorDetail;
import com.omnistack.backend.application.port.in.BusinessLinesUseCase;
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
 * Controller REST para consulta de lineas de negocio.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Business Lines", description = "Consulta de oferta comercial parametrica")
public class BusinessLinesController {

    private final BusinessLinesUseCase businessLinesUseCase;

    @PostMapping(ApiPaths.BUSINESS_LINES)
    @Operation(
            summary = "Consultar lineas de negocio",
            description = "Retorna la oferta comercial parametrizada para un punto de venta",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Consulta exitosa",
                        content = @Content(
                                examples = @ExampleObject(value = """
                                        {
                                          "chain": "1",
                                          "store": "148",
                                          "store_name": "FYBECA AMAZONAS",
                                          "pos": "1",
                                          "channel_POS": "POS",
                                          "collection_subcategory": [
                                            {
                                              "category_code": "REC",
                                              "category_name": "Recargas",
                                              "subcategory_code": "CEL",
                                              "subcategory_name": "Recargas celulares",
                                              "is_active": true,
                                              "service_providers": [
                                                {
                                                  "service_provider_code": "CLARO",
                                                  "provider_name": "Claro",
                                                  "is_active": true,
                                                  "services": [
                                                    {
                                                      "rms_item_code": "900001",
                                                      "description": "Recarga Claro",
                                                      "is_active": true,
                                                      "jde_code": "JDE-REC-001",
                                                      "movement_type": "CASH_IN",
                                                      "is_mixed_payment": false,
                                                      "flg_item": "RECA",
                                                      "is_refund": false,
                                                      "min_amount": "1.00",
                                                      "max_amount": "200.00",
                                                      "timeout_ws_max": "10000",
                                                      "retries_ws_max": "3",
                                                      "capabilities": ["PRECHECK", "EXECUTE", "VERIFY", "REVERSE"],
                                                      "input_fields": [
                                                        {
                                                          "id": "phone",
                                                          "label": "Telefono",
                                                          "type": "STRING",
                                                          "capability": "PRECHECK",
                                                          "required": true,
                                                          "group": "PHONE"
                                                        }
                                                      ],
                                                      "payment_methods": [
                                                        {
                                                          "service_payment_method_id": 1,
                                                          "payment_method_code": "EFECTIVO",
                                                          "is_active": true
                                                        }
                                                      ],
                                                      "requires_consent": false
                                                    }
                                                  ]
                                                }
                                              ]
                                            }
                                          ]
                                        }
                                        """))),
                @ApiResponse(responseCode = "400", description = "Solicitud invalida", content = @Content(schema = @Schema(implementation = ErrorDetail.class)))
            })
    public ResponseEntity<BusinessLinesResponse> getBusinessLines(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "chain":"1",
                              "store":"148",
                              "store_name":"FYBECA AMAZONAS",
                              "pos":"1",
                              "channel_POS":"POS",
                              "movement_type_filter":"CASH_IN"
                            }
                            """)))
            @Valid @RequestBody BusinessLinesRequest request) {
        return ResponseEntity.ok(businessLinesUseCase.getBusinessLines(request));
    }
}
