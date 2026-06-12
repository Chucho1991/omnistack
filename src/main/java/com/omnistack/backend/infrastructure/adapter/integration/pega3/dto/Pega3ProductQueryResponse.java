package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Response externo de consulta de producto Pega3 (VentaProductos).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pega3ProductQueryResponse {
    private String code;
    private String name;
    private List<String> entryTypes;
    private List<BigDecimal> betAmountOptions;
    private BigDecimal minCost;
    private Integer retailerCancelPeriod;
    private String message;
}
