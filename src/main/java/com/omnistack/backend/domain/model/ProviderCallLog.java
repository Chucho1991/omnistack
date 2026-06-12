package com.omnistack.backend.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProviderCallLog {
    String uuid;
    String providerKey;
    String wsKey;
    String url;
    String requestJson;
    String responseJson;
    Long durationMs;
    Integer httpStatus;
    boolean isError;
    String errorMessage;
}
