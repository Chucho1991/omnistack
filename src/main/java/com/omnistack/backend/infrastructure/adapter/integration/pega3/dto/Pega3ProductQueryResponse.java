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

    /** Array de objetos en el proveedor real (no strings) — configuracion por modalidad de entrada. */
    private List<EntryType> entryTypes;

    /** El proveedor real envia un CSV ("0.5,1,5,10,20,30"), no un array. */
    private String betAmountOptions;

    private BigDecimal minCost;
    private Integer retailerCancelPeriod;
    private String message;

    /** Monto inicial del premio a partir del cual el proveedor exige control de riesgo (RN-07). */
    private BigDecimal prizeLiabilityThreshold;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EntryType {
        private String code;
        private String name;
        private BigDecimal minWager;
        private BigDecimal maxWager;
        private BigDecimal playerSpendingLimit;
        private Integer minWagerColumns;
        private Integer maxWagerColumns;
        private Boolean supportQuickPick;
        private Integer minQuickPickCombinations;
        private Integer maxQuickPickCombinations;
        private Boolean supportAdvanceDraw;
        private Integer advanceDrawLimit;
        private Integer futureDrawsLimit;
        private List<String> playTypes;
    }
}
