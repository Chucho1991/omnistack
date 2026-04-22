package com.omnistack.backend.domain.model;

import com.omnistack.backend.domain.enums.Capability;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

/**
 * Request normalizado para proveedores externos.
 */
@Value
@Builder
public class ExternalTransactionRequest {
    String uuid;
    String providerCode;
    Capability capability;
    Map<String, Object> payload;
}
