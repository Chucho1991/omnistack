package com.omnistack.backend.domain.model;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Response normalizado de proveedores externos.
 */
@Value
@Builder
public class ExternalTransactionResponse {
    boolean approved;
    String externalCode;
    String externalMessage;
    Map<String, Object> payload;
}
