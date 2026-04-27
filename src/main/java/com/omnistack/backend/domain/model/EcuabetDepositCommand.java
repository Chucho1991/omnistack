package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.ChannelPos;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno del dominio para la operacion de deposito de usuario en ECUABET.
 */
@Value
@Builder
public class EcuabetDepositCommand {
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
    String document;
    BigDecimal amount;
    Integer transactionId;
}
