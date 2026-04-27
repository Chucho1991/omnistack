package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno del dominio para la recarga de saldo BET593 por Loteria.
 */
@Value
@Builder
public class Bet593RechargeCommand {
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
    String userid;
    String phone;
    String withdrawId;
    String password;
    String authorization;
    String serialnumber;
    String document;
    BigDecimal amount;
}
