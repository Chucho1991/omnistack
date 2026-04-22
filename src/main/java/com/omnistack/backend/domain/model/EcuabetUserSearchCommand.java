package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.ChannelPos;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

/**
 * Contrato interno para la operacion Buscar usuario de ECUABET.
 */
@Value
@Builder
public class EcuabetUserSearchCommand {
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
    String withdrawId;
    String password;
    BigDecimal amount;
}
