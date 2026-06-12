package com.omnistack.backend.infrastructure.adapter.integration.pega3.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Value;

/**
 * Request externo para crear ticket de apuesta Pega3 (CrearTicket).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Pega3CreateTicketRequest {
    String deviceId;
    String token;
    String productoVender;
    String customerSessionId;
    BigDecimal cost;
    String entryType;
    String channel;
    MainGame mainGame;

    /**
     * Estructura del juego principal dentro del ticket.
     */
    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MainGame {
        String code;
        Integer advanceDraw;
        Integer noOfDraws;
        List<Panel> panels;
        List<Object> addOns;
    }

    /**
     * Panel individual de apuesta.
     */
    @Value
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Panel {
        String betType;
        Integer typeOfEntry;
        BigDecimal betAmount;
        List<Entry> entries;
    }

    /**
     * Entrada de numeros seleccionados dentro de un panel.
     */
    @Value
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(force = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Entry {
        Integer type;
        Boolean quickPick;
        List<String> playTypes;
        List<Integer> value;
    }
}
