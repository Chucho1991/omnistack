package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.ChannelPos;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno del dominio para la ejecucion de nota de retiro ECUABET.
 */
@Value
@Builder
public class EcuabetWithdrawCommand {
    String uuid;
    String chain;
    String store;
    String storeName;
    String pos;
    ChannelPos channelPos;
    String categoryCode;
    String subcategoryCode;
    String serviceProviderCode;
    String rmsItemCode;
    String userid;
    String phone;
    String withdrawId;
    String password;
    String document;
    BigDecimal amount;
    Integer transactionId;
}
