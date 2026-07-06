package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaroPrecheckCommand {
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
    String phone;
    String amount;
    String offerId;
    // campos resueltos desde AD_ITEM_SERVICIO y PROVEEDOR_CONFIG
    String companyId;
    String externalOperation;
    String mediaId;
    String codCaja;
    String codSite;
}
