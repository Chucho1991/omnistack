package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import lombok.Builder;
import lombok.Value;

/**
 * Request externo para el endpoint de reverso de nota de retiro ECUABET.
 */
@Value
@Builder
public class EcuabetWithdrawReverseRequest {
    String shop;
    String token;
    Integer country;
    String withdrawId;
    String password;
    Integer transactionId;
}
