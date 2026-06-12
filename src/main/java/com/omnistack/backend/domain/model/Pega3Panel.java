package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Panel de apuesta dentro de un ticket Pega3.
 */
@Value
@Builder
public class Pega3Panel {
    BigDecimal betAmount;
    List<Integer> numbers;
    List<String> playTypes;
}
