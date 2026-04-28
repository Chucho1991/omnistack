package com.omnistack.backend.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno del dominio para la ejecucion de nota de retiro BET593 por Loteria.
 */
@Value
@Builder
public class Bet593WithdrawCommand {
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
    String motivo;
    BigDecimal amount;
}
