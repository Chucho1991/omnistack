package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para consultar numeros disponibles (RecuperarNumerosDisponiblesPorCombinacion - PRECHECK step 4).
 */
@Value
@Builder
public class TradicionalNumerosQueryCommand {
    String uuid;
    String chain;
    String store;
    String storeName;
    String pos;
    String channelPos;
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String rmsItemCode;
    Integer medioId;
    String userName;
    String juegoId;
    String sorteoId;
    String combinacion;
    String combinacionFigura;
    Boolean sugerir;
    Integer cantidad;
    Integer registros;
}
