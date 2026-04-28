package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para el endpoint de reverso de deposito ECUABET.
 */
@Value
@Builder
public class EcuabetDepositReverseRequest {
    String shop;
    String token;
    Integer country;
    BigDecimal amount;
    Integer transactionId;
}
