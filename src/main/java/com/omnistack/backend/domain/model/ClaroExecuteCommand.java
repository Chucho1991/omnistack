package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaroExecuteCommand {
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
    String authorizationNumber;
    String companyId;
    String externalOperation;
    String consumerId;
    String channelId;
    String mediaId;
    String mediaDetailId;
    String subscriberType;
    String subscriptionType;
    String codCaja;
    String codSite;
}
