package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para el endpoint de deposito ECUABET.
 */
@Value
@Builder
public class EcuabetDepositRequest {
    String shop;
    String token;
    Long userid;
    Integer country;
    BigDecimal amount;
    Integer transactionId;
    @JsonProperty("shop_info")
    String shopInfo;
    @JsonProperty("shop_ip")
    String shopIp;
}
