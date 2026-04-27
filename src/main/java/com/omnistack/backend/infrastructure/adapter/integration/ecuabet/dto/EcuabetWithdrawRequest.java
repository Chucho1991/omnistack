package com.omnistack.backend.infrastructure.adapter.integration.ecuabet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

/**
 * Request externo para el endpoint de ejecucion de nota de retiro ECUABET.
 */
@Value
@Builder
public class EcuabetWithdrawRequest {
    String shop;
    String token;
    String withdrawId;
    Integer country;
    String password;
    Integer transactionId;
    @JsonProperty("shop_info")
    String shopInfo;
    @JsonProperty("shop_ip")
    String shopIp;
}
